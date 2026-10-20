#!/usr/bin/env bash
# Deterministic resource conversion; never modifies or redraws the source artwork.
set -euo pipefail
cd "$(dirname "$0")/.."
source_art='assets/Cozy Neon Battery Hug.png'
res=app/src/main/res
for entry in mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432; do
    IFS=: read -r density legacy adaptive <<< "$entry"
    mkdir -p "$res/mipmap-$density" "$res/drawable-$density"
    # Entire original fits within Android's 66dp circular adaptive safe area.
    art_size=$((adaptive * 46 / 108))
    magick "$source_art" -resize "${art_size}x${art_size}" -background '#090C09' -gravity center -extent "${adaptive}x${adaptive}" -strip "$res/drawable-$density/ic_launcher_foreground.png"
    art_size=$((legacy * 70 / 100))
    radius=$((legacy * 22 / 100))
    magick "$source_art" -resize "${art_size}x${art_size}" -background '#090C09' -gravity center -extent "${legacy}x${legacy}"         \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" -fill white -draw "roundrectangle 0,0 ${legacy},${legacy} ${radius},${radius}" \)         -alpha off -compose CopyOpacity -composite -strip "$res/mipmap-$density/ic_launcher.png"
    magick "$res/mipmap-$density/ic_launcher.png" \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" -fill white -draw "circle $((legacy/2)),$((legacy/2)) $((legacy/2)),0" \) -alpha off -compose CopyOpacity -composite -strip "$res/mipmap-$density/ic_launcher_round.png"
done
