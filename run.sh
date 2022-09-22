#!/bin/sh
docker run --detach -e BEETS_ALLOWED_CHANNEL=$(sh beets_channels.sh) -v /srv/beets/prod.secret:/run/beets_discord_token -v /srv/beets/beets.sqlite:/app/beets.sqlite:rw --restart=always -it --name=beets beets
