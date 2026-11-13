# Ассеты Teleport

Дизайн: кольцо портала с разрывом и вылетающей стрелкой на тёмно-индиговом
фоне. Палитра: фон `#1E1B4B` → `#312E81` (радиальный), кольцо `#22D3EE` →
`#A78BFA` (линейный по диагонали), стрелка `#F8FAFC`. Геометрия единая для
launcher-иконки и Play: кольцо r=128/512 (27/108 для adaptive), разрыв ±35°,
всё внутри safe zone adaptive icon (66/108).

## Файлы и куда их класть

| Файл | Назначение |
|---|---|
| `icon/adaptive/ic_launcher.xml`, `ic_launcher_round.xml` | → `app/src/main/res/mipmap-anydpi-v26/` |
| `icon/adaptive/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` | → `app/src/main/res/drawable/` |
| `icon/teleport-icon-512.png` | Play Console: иконка листинга 512×512 |
| `icon/feature-graphic.png` | Play Console: feature graphic 1024×500 |
| `icon/teleport-icon.svg` | исходник иконки 512×512 |
| `icon/teleport-monochrome.svg` | исходник monochrome (прозрачный фон) |
| `icon/feature-graphic.svg` | исходник feature graphic без текста (текст накладывается при рендере) |
| `icon/preview-circle.png` | превью: круглая маска лаунчера (safe zone 66/108) |
| `icon/preview-monochrome.png` | превью: themed icon (Android 13+) на сером |

F-Droid использует launcher-иконку из res; отдельных ассетов не требует.
Скриншоты листинга готовятся на этапе публикации, когда приложение собрано.

## Регенерация

Важно: внутренний SVG-рендерер ImageMagick (MSVG) не поддерживает градиенты —
не рендерить им SVG. Рабочие варианты на macOS:

```bash
# иконка 512 (Quick Look, WebKit-рендер):
qlmanage -t -s 512 -o <outdir> icon/teleport-icon.svg

# feature graphic: точный рендер cairosvg + текст через magick
python3 -m venv /tmp/teleport-venv
/tmp/teleport-venv/bin/pip install cairosvg
DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib /tmp/teleport-venv/bin/python -c \
  "import cairosvg; cairosvg.svg2png(url='icon/feature-graphic.svg', write_to='/tmp/feature-bg.png', output_width=1024, output_height=500)"
magick /tmp/feature-bg.png \
  -font "/System/Library/Fonts/Supplemental/Arial Bold.ttf" -pointsize 96 -fill "#F8FAFC" \
  -gravity West -annotate +90-15 "Teleport" \
  -font "/System/Library/Fonts/Supplemental/Arial.ttf" -pointsize 40 -fill "#C7D2FE" \
  -annotate +92+65 "One icon. Any app." icon/feature-graphic.png
```

`qlmanage` вписывает неквадратные SVG в квадратный холст с полями — для
1024×500 использовать cairosvg.
