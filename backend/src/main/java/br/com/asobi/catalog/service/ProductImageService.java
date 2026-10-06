package br.com.asobi.catalog.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.asobi.catalog.dto.ProductImageView;
import br.com.asobi.catalog.model.Product;
import br.com.asobi.catalog.model.ProductImage;
import br.com.asobi.catalog.repository.ProductRepository;
import br.com.asobi.common.exception.BusinessException;
import br.com.asobi.common.exception.NotFoundException;
import br.com.asobi.storage.ImageStorage;
import br.com.asobi.storage.StorageException;

/** Fotos dos produtos: confere os arquivos enviados pelo painel e guarda no storage. */
@Service
@Transactional
public class ProductImageService {

	public static final int MAX_IMAGES = 6;
	public static final long MAX_BYTES = 5L * 1024 * 1024;

	private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

	private final ProductRepository productRepository;
	private final ImageStorage storage;

	public ProductImageService(ProductRepository productRepository, ImageStorage storage) {
		this.productRepository = productRepository;
		this.storage = storage;
	}

	@Transactional(readOnly = true)
	public List<ProductImageView> list(Long productId) {
		return inGalleryOrder(getProduct(productId)).stream()
				.map(image -> new ProductImageView(image.getId(), storage.publicUrl(image.getStorageKey())))
				.toList();
	}

	/** URLs públicas na ordem da galeria (a primeira é a capa). */
	public List<String> urls(Product product) {
		return inGalleryOrder(product).stream()
				.map(image -> storage.publicUrl(image.getStorageKey()))
				.toList();
	}

	/** Ordena aqui também: a lista já carregada não se reordena sozinha quando a capa muda. */
	private static List<ProductImage> inGalleryOrder(Product product) {
		return product.getImages().stream()
				.sorted(Comparator.comparingInt(ProductImage::getPosition))
				.toList();
	}

	/**
	 * Acrescenta fotos ao produto (que já precisa ter id). Confere todos os arquivos
	 * antes de gravar o primeiro, para não ficar com o envio pela metade.
	 */
	public void add(Product product, List<MultipartFile> files) {
		List<ProductImage> images = product.getImages();
		List<Upload> uploads = check(images.size(), files);
		if (uploads.isEmpty()) {
			return;
		}

		int position = images.stream().mapToInt(ProductImage::getPosition).max().orElse(-1) + 1;
		List<String> stored = new ArrayList<>();
		try {
			for (Upload upload : uploads) {
				String key = "products/" + product.getId() + "/" + UUID.randomUUID() + "." + upload.type().extension;
				storage.store(key, upload.content(), upload.type().contentType);
				stored.add(key);
				images.add(new ProductImage(product, key, position++));
			}
		} catch (StorageException ex) {
			log.error("Falha ao guardar foto do produto {}", product.getId(), ex);
			stored.forEach(this::deleteQuietly);
			throw new BusinessException("Não foi possível salvar as fotos agora. Tente novamente em instantes.");
		}
	}

	/** Confere as fotos de um produto novo antes de ele ser gravado. */
	public void validateForNewProduct(List<MultipartFile> files) {
		check(0, files);
	}

	/** Quantidade, tamanho e tipo; devolve só os arquivos realmente enviados. */
	private static List<Upload> check(int currentCount, List<MultipartFile> files) {
		List<MultipartFile> photos = files == null ? List.of()
				: files.stream().filter(file -> !file.isEmpty()).toList();
		if (currentCount + photos.size() > MAX_IMAGES) {
			throw new BusinessException("Cada produto pode ter até " + MAX_IMAGES + " fotos"
					+ (currentCount == 0 ? "." : " — este já tem " + currentCount + "."));
		}
		return photos.stream().map(ProductImageService::read).toList();
	}

	public void delete(Long productId, Long imageId) {
		Product product = getProduct(productId);
		ProductImage image = findImage(product, imageId);
		product.getImages().remove(image);
		deleteQuietly(image.getStorageKey());
	}

	/** Passa a foto para o começo da galeria; as outras mantêm a ordem. */
	public void makeCover(Long productId, Long imageId) {
		Product product = getProduct(productId);
		ProductImage cover = findImage(product, imageId);
		List<ProductImage> ordered = new ArrayList<>(inGalleryOrder(product));
		ordered.remove(cover);
		ordered.add(0, cover);
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).setPosition(i);
		}
	}

	/** Apaga os arquivos de um produto que está sendo excluído (as linhas saem em cascata). */
	public void deleteFiles(Product product) {
		product.getImages().forEach(image -> deleteQuietly(image.getStorageKey()));
	}

	private Product getProduct(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Produto não encontrado."));
	}

	private static ProductImage findImage(Product product, Long imageId) {
		return product.getImages().stream()
				.filter(image -> image.getId().equals(imageId))
				.findFirst()
				.orElseThrow(() -> new NotFoundException("Foto não encontrada."));
	}

	/** Um arquivo que sobra no storage não quebra a loja; só registra. */
	private void deleteQuietly(String key) {
		try {
			storage.delete(key);
		} catch (RuntimeException ex) {
			log.warn("Não foi possível apagar a foto {} do storage", key, ex);
		}
	}

	private static Upload read(MultipartFile file) {
		String name = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
		if (file.getSize() > MAX_BYTES) {
			throw new BusinessException("\"" + name + "\" é grande demais (máximo de 5 MB por foto).");
		}
		byte[] content;
		try {
			content = file.getBytes();
		} catch (IOException ex) {
			throw new BusinessException("Não foi possível ler \"" + name + "\". Tente enviar de novo.");
		}
		ImageType type = ImageType.detect(content);
		if (type == null) {
			throw new BusinessException("\"" + name + "\" não é uma foto válida. Use JPG, PNG ou WebP.");
		}
		return new Upload(content, type);
	}

	private record Upload(byte[] content, ImageType type) {
	}

	/** Tipo descoberto pelo começo do arquivo — não confia na extensão nem no navegador. */
	enum ImageType {
		JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

		private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

		final String extension;
		final String contentType;

		ImageType(String extension, String contentType) {
			this.extension = extension;
			this.contentType = contentType;
		}

		static ImageType detect(byte[] content) {
			if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
					&& (content[2] & 0xFF) == 0xFF) {
				return JPEG;
			}
			if (startsWith(content, 0, PNG_SIGNATURE)) {
				return PNG;
			}
			if (startsWith(content, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
					&& startsWith(content, 8, "WEBP".getBytes(StandardCharsets.US_ASCII))) {
				return WEBP;
			}
			return null;
		}

		private static boolean startsWith(byte[] content, int offset, byte[] signature) {
			if (content.length < offset + signature.length) {
				return false;
			}
			for (int i = 0; i < signature.length; i++) {
				if (content[offset + i] != signature[i]) {
					return false;
				}
			}
			return true;
		}
	}
}
