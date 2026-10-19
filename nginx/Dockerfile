# Nginx do convite: HTTPS na 443 (publicado como 3041 na VPS) e proxy da API para o backend.
# O site (React) fica na Vercel; aqui só passa a API.
FROM nginx:1.29-alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/convite.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
EXPOSE 443
