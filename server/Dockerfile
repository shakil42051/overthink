FROM node:24-bookworm-slim
ENV NODE_ENV=production HOST=0.0.0.0 PORT=8787 DATABASE_PATH=/data/overthink.sqlite
WORKDIR /app
COPY --chown=node:node server.mjs package.json manage.mjs ./
RUN mkdir /data && chown node:node /data
USER node
EXPOSE 8787
VOLUME /data
CMD ["node", "server.mjs"]
