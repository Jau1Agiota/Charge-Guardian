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
    # Fill the 72dp visible viewport; the source already includes its own frame.
    # A 2dp downward optical offset protects the crown under the circular mask.
    art_size=$((adaptive * 72 / 108))
    shift_y=$((adaptive * 2 / 108))
    magick -size "${adaptive}x${adaptive}" xc:none \
        \( "$source_art" -resize "${art_size}x${art_size}" \) -gravity center -geometry "+0+${shift_y}" \
        -compose over -composite -depth 8 -strip "$res/drawable-$density/ic_launcher_foreground.png"
    # Same composition at legacy size; no second 70% shrink or nested mask.
    shift_y=$((legacy * 2 / 72))
    magick -size "${legacy}x${legacy}" xc:'#090C09' \
        \( "$source_art" -resize "${legacy}x${legacy}" \) -gravity center -geometry "+0+${shift_y}" \
        -compose over -composite -depth 8 -strip "$scratch/legacy.png"
    radius=$((legacy * 22 / 100))
    magick "$scratch/legacy.png" \
        \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" \
        -fill white -draw "roundrectangle 0,0 ${legacy},${legacy} ${radius},${radius}" \) \
        -alpha off -compose CopyOpacity -composite -depth 8 -strip "$res/mipmap-$density/ic_launcher.png"
    magick "$scratch/legacy.png" \
        \( +clone -alpha extract -fill black -draw "rectangle 0,0 ${legacy},${legacy}" \
        -fill white -draw "circle $((legacy/2)),$((legacy/2)) $((legacy/2)),0" \) \
        -alpha off -compose CopyOpacity -composite -depth 8 -strip "$res/mipmap-$density/ic_launcher_round.png"
done
