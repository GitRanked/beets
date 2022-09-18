docker run --detach -e BEETS_ALLOWED_CHANNEL=439292332144066560 -v /srv/beets/prod.secret:/run/beets_discord_token -v /srv/beets/beets.sqlite:/app/beets.sqlite:rw --restart=always -it beets
