#
# conductor:ui - Netflix conductor UI
#
FROM node:16.0.0-alpine
MAINTAINER Netflix OSS <conductor@netflix.com>

# Install the required packages for the node build
# to run on alpine
# RUN apk update && apk add \
#   autoconf \
#   automake \
#   libtool \
#   build-base \
#   libstdc++ \
#   gcc \
#   abuild \
#   binutils \
#   nasm \
#   libpng \
#   libpng-dev \
#   libjpeg-turbo \
#   libjpeg-turbo-dev \
#   python

# Make app folders
RUN mkdir -p /app/ui

# Copy the ui files onto the image
COPY ./docker/ui/bin /app
COPY ./ui/dist /app/ui/dist
COPY ./ui/node_modules /app/ui/node_modules

# Copy the files for the server into the app folders
RUN chmod +x /app/startup.sh

# Get and install conductor UI
# RUN yarn config set registry https://registry.npm.taobao.org
# RUN cd /app/ui \
#   && rm package-lock.json \
#   && yarn \
#   && yarn run build --server

EXPOSE 5000

CMD [ "/app/startup.sh" ]
ENTRYPOINT ["/bin/sh"]
