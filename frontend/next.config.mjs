const API_URL = (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080").replace(/\/$/, "");

/** @type {import('next').NextConfig} */
const nextConfig = {
  // O painel admin é o do backend (Spring Boot + Thymeleaf); /admin na loja só leva até lá.
  async redirects() {
    return [
      { source: "/admin", destination: `${API_URL}/admin`, permanent: false },
      { source: "/admin/:path*", destination: `${API_URL}/admin`, permanent: false },
      // A conta é criada no primeiro login com Google; não há tela de cadastro separada.
      { source: "/cadastro", destination: "/login", permanent: false },
    ];
  },
};

export default nextConfig;
