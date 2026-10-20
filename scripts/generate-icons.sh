#!/usr/bin/env bash
# Proportional resizes only: preserve the source illustration; mask legacy icons once.
set -euo pipefail
cd "$(dirname "$0")/.."
source_art='assets/Cozy Neon Battery Hug.png'
res=app/src/main/res
scratch=$(mktemp -d /tmp/charge-guardian-icons.XXXXXX)
trap 'rm -rf "$scratch"' EXIT
for entry in mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432; do
    IFS=: read -r density legacy adaptive <<< "$entry"
    mkdir -p "$res/mipmap-$density" "$res/drawable-$density"
    # Android displays the central 72dp of a 108dp adaptive layer.
    # A 58dp illustration fills ~81% of that window, versus 64% in Stage 1.1.
    art_size=$((adaptive * 58 / 108))
    magick "$source_art" -resize "${art_size}x${art_size}" -background none -gravity center \
        -extent "${adaptive}x${adaptive}" -strip "$res/drawable-$density/ic_launcher_foreground.png"
    # Same composition at legacy size; no second 70% shrink or nested mask.
    art_size=$((legacy * 58 / 72))
    magick "$source_art" -resize "${art_size}x${art_size}" -background '#090C09' -gravity center \
        -extent "${legacy}x${legacy}" -strip "$scratch/legacy.png"
    radius=$((legacy * 22 / 100))
    magick "$scratch/legacy.png" \
        \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" \
        -fill white -draw "roundrectangle 0,0 ${legacy},${legacy} ${radius},${radius}" \) \
        -alpha off -compose CopyOpacity -composite -strip "$res/mipmap-$density/ic_launcher.png"
    magick "$scratch/legacy.png" \
        \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" \
        -fill white -draw "circle $((legacy/2)),$((legacy/2)) $((legacy/2)),0" \) \
        -alpha off -compose CopyOpacity -composite -strip "$res/mipmap-$density/ic_launcher_round.png"
done
