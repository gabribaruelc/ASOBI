package br.com.asobi.status.check;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.asobi.status.check.CheckService.TargetResult;

/** Chamado pelo Cloud Scheduler a cada 5 minutos. */
@RestController
@RequestMapping("/api")
public class CheckController {

	private final CheckService checkService;

	public CheckController(CheckService checkService) {
		this.checkService = checkService;
	}

	@PostMapping("/checks")
	public List<TargetResult> run() {
		return checkService.run();
	}
}
