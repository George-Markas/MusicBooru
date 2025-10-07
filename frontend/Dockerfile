FROM nginx:alpine

RUN rm -rf /usr/share/nginx/html/*

COPY *.html /usr/share/nginx/html/
COPY *.js /usr/share/nginx/html/

COPY nginx-frontend.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]