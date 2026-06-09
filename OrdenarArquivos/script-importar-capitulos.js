// ==UserScript==
// @name         OrdenarArquivos — Importar Capítulos
// @namespace    http://tampermonkey.net/
// @version      1.3.0
// @description  Extrai lista de capítulos da página e copia no formato do popup Colar do OrdenarArquivos
// @author       Fenix
// @grant        GM_setClipboard
//
// Descomente os @match dos sites que deseja usar ao instalar no Tampermonkey:
//
// @match        https://comick.io/*
// @match        https://comickfan.com/*
// @match        https://comick.dev/*
// @match        https://mangafire.to/*
// @match        https://mangak.io/*
// @match        https://www.mangaread.org/*
// @match        https://mangaread.org/*
// @match        https://mangadex.org/*
// @match        https://taiyo.moe/*
// @match        https://mangapark.net/*
// @match        https://www.zazamanga.com/*
// @match        https://zazamanga.com/*
// @match        https://mangakatana.com/*
// @match        https://mangaforest.me/*
// @match        https://mangaplanet.com/*
// @match        https://vymanga.net/*
// @match        https://www.mangatown.com/*
// @match        https://m.mangatown.com/*
// @match        https://www.mangahere.cc/*
// @match        https://kmanga.kodansha.com/*
//
// ==/UserScript==

(function () {
    'use strict';

    const PREFERRED_LANG = 'portuguese';
    const TITLE_CLEAN_RE = /^(ch|chapter|episode|第|[0-9])[0-9０-９ .]+(話|:)?/i;
    const LIMPAR_TITULO_RE = /(capítulo|capitulo|chapter|ch\.?|話)\s*[\d.]+\s*[\-—:]?\s*/gi;
    const BTN_CONTAINER_ID = 'oa-capitulos-container';
    const BTN_ID = 'oa-btn-extrair-capitulos';

    // -------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------

    function parseNum(s) {
        if (s == null || s === '') return null;
        const n = parseFloat(String(s).trim());
        return Number.isFinite(n) ? n : null;
    }

    function formatVolNum(n) {
        return Number.isInteger(n) ? String(Math.trunc(n)) : String(n);
    }

    function formatChapterNum(n) {
        return formatVolNum(n);
    }

    function cleanTitle(title) {
        if (!title) return '';
        return String(title).replace(TITLE_CLEAN_RE, '').trim();
    }

    function limparTitulo(titulo) {
        if (!titulo) return '';
        return String(titulo).replace(LIMPAR_TITULO_RE, '').trim();
    }

    function cleanImportedTitle(rawTitle) {
        const trimmed = String(rawTitle || '').trim();
        if (!trimmed) return '';
        let cleaned = trimmed.replace(TITLE_CLEAN_RE, '').trim();
        if (!cleaned) cleaned = limparTitulo(trimmed).replace(TITLE_CLEAN_RE, '').trim();
        if (!cleaned) {
            const suffixMatch = /(?:Chapter|Capítulo|Ch\.?)\s*[\d.]+\s*[:\-—]?\s*(.+)/i.exec(trimmed);
            cleaned = suffixMatch ? suffixMatch[1].trim() : '';
        }
        return cleaned;
    }

    function volumeChapterKey(volume, chapter) {
        return `${volume}:${chapter}`;
    }

    function addCapituloSeValido(volumesMap, volNum, chapNum, title, requireTitle = true, processed = null) {
        if (processed) {
            const key = volumeChapterKey(volNum, chapNum);
            if (processed.has(key)) return false;
            processed.add(key);
        }
        const cleaned = cleanImportedTitle(title);
        if (requireTitle && !cleaned) return false;
        if (!volumesMap[volNum]) volumesMap[volNum] = { chapters: [] };
        volumesMap[volNum].chapters.push({ num: chapNum, title: cleaned });
        return true;
    }

    function mangadexLangTier(langImg) {
        if (!langImg) return null;
        const title = (langImg.getAttribute('title') || '').toLowerCase();
        const alt = (langImg.getAttribute('alt') || '').toLowerCase();
        const src = (langImg.getAttribute('src') || '').toLowerCase();
        const combined = `${title} ${alt} ${src}`;
        if (combined.includes('brazil') || combined.includes('portuguese') ||
            src.includes('br.svg') || src.includes('pt.svg')) return 'pt';
        if (combined.includes('english') || src.includes('gb.svg') || src.includes('us.svg')) return 'en';
        if (combined.includes('spanish') || src.includes('es.svg')) return 'es';
        return null;
    }

    function chapterLine(num, title) {
        const n = formatChapterNum(num);
        const t = cleanTitle(title);
        return t ? `Chapter ${n}: ${t}` : `Chapter ${n}`;
    }

    /**
     * @param {Array<{volume: number, chapters: Array<{num: number, title: string}>, keepOrder?: boolean}>} volumes
     */
    function formatVolumesForPaste(volumes) {
        if (!volumes || volumes.length === 0) return '';

        const sorted = [...volumes].sort((a, b) => a.volume - b.volume);
        const singleNoVol = sorted.length === 1 && sorted[0].volume < 0;
        const blocks = [];

        for (const vol of sorted) {
            const lines = [];
            const printHeader = !singleNoVol && vol.volume >= 0;
            if (printHeader) {
                lines.push(`Volume ${formatVolNum(vol.volume)}`);
            }

            const chapters = [...vol.chapters];
            if (!vol.keepOrder) {
                chapters.sort((a, b) => a.num - b.num);
            }
            for (const ch of chapters) {
                lines.push(chapterLine(ch.num, ch.title));
            }
            blocks.push(lines.join('\n'));
        }

        return blocks.join('\n\n').trim();
    }

    function volumesMapToList(volumesMap, keepOrder) {
        return Object.keys(volumesMap)
            .map(k => parseFloat(k))
            .sort((a, b) => a - b)
            .map(vol => ({
                volume: vol,
                chapters: volumesMap[vol].chapters,
                keepOrder: keepOrder || volumesMap[vol].keepOrder
            }));
    }

    function addChapter(volumesMap, volNum, chapNum, title) {
        const key = volNum;
        if (!volumesMap[key]) {
            volumesMap[key] = { chapters: [] };
        }
        volumesMap[key].chapters.push({ num: chapNum, title: cleanTitle(title) });
    }

    const CHAPTER_LINE_REGEX = /(?:Vol(?:ume)?\.?\s*(\d+(?:\.\d+)?))?\s*(?:Chapter|Ch\.?|Chap|Capítulo|Capitulo|Cap\.?|C\.?|第)?\s*([\d.]+)\s*[:\-—]?\s*(.*)/i;
    const KMANGA_CHAPTER_REGEX = /CHAPTER(\d+(?:\.\d+)?)\s*(.*)/i;
    const MANGATOWN_DESKTOP_CHAPTER_REGEX = /(\d+(?:\.\d+)?)$/;
    const MANGATOWN_MOBILE_CHAPTER_REGEX = /C\.(\d+(?:\.\d+)?)/i;

    function parseChapterFromText(text) {
        const trimmed = String(text || '').trim();
        if (!trimmed) return null;
        const match = CHAPTER_LINE_REGEX.exec(trimmed);
        if (!match) return null;
        const num = parseNum(match[2]);
        if (num == null) return null;
        const volume = match[1] ? parseNum(match[1]) : null;
        return { volume, num, title: (match[3] || '').trim() };
    }

    function parseKMangaChapterText(text) {
        const trimmed = String(text || '').trim();
        if (!trimmed) return null;
        const match = KMANGA_CHAPTER_REGEX.exec(trimmed);
        if (!match) return null;
        const num = parseNum(match[1]);
        if (num == null) return null;
        return { volume: null, num, title: (match[2] || '').trim() };
    }

    function addParsedChapter(volumesMap, processed, parsed, cleanTitleFlag = true) {
        if (processed.has(parsed.num)) return;
        let title = parsed.title || '';
        if (cleanTitleFlag) title = limparTitulo(title);
        const cleaned = cleanTitleFlag ? cleanImportedTitle(parsed.title) : cleanImportedTitle(title);
        if (!cleaned) return;
        processed.add(parsed.num);
        const volNum = parsed.volume != null ? parsed.volume : -1;
        addChapter(volumesMap, volNum, parsed.num, cleaned);
    }

    // -------------------------------------------------------------------------
    // Comick — buildVolumesFromTempCaps
    // -------------------------------------------------------------------------

    function buildVolumesFromTempCaps(rawEntries) {
        const finalMap = {};
        const sorted = [...rawEntries].sort((a, b) => a.sortKey - b.sortKey);

        for (const current of sorted) {
            const existing = finalMap[current.chap];
            if (!existing) {
                finalMap[current.chap] = current;
            } else {
                const curHas = current.title && current.title.trim();
                const exHas = existing.title && existing.title.trim();
                if (curHas && !exHas) {
                    finalMap[current.chap] = current;
                } else if (curHas === exHas) {
                    if (current.vol != null && existing.vol == null) {
                        finalMap[current.chap] = current;
                    }
                }
            }
        }

        const volumesMap = {};
        Object.values(finalMap)
            .sort((a, b) => a.chap - b.chap)
            .forEach(entry => {
                const cleaned = cleanImportedTitle(entry.title);
                if (!cleaned) return;
                const volNum = entry.vol != null ? entry.vol : -1;
                addChapter(volumesMap, volNum, entry.chap, cleaned);
            });

        return volumesMapToList(volumesMap);
    }

    function parseComickRow(row, index) {
        const chapSpan = row.querySelector(
            'td span.font-bold, td span.font-semibold, td span.font-medium, ' +
            'span.font-bold, span.font-semibold, span.font-medium'
        );
        if (!chapSpan) return null;

        const container = chapSpan.parentElement;
        if (!container) return null;

        const spans = Array.from(container.children).filter(el => el.tagName === 'SPAN');
        if (spans.length === 0) return null;

        const capRegex = /(?:(?:Chapter|Capítulo|Ch\.?|Cap\.?|第)\s*)?(\d+(?:\.\d+)?)/i;
        const chapSpanText = spans[0].textContent.trim().replace(/,$/, '');
        const capMatch = capRegex.exec(chapSpanText);
        if (!capMatch) return null;
        const chapNum = parseNum(capMatch[1]);
        if (chapNum == null) return null;

        let volNum = null;
        let title = '';
        const volRegex = /(?:Vol|Volume)\.?\s*(\d+(?:\.\d+)?)/i;

        for (let i = 1; i < spans.length; i++) {
            const spanText = spans[i].textContent.trim();
            if (/vol\.?|volume/i.test(spanText)) {
                const volMatch = volRegex.exec(spanText);
                if (volMatch) volNum = parseNum(volMatch[1]);
            } else if (spanText) {
                title = spanText;
            }
        }

        if (!title) {
            const link = row.querySelector('a.link, a[href*="/comic/"]');
            title = link ? link.textContent.trim() : '';
        }

        return { vol: volNum, chap: chapNum, title, sortKey: index };
    }

    function extractComickFromEmbeddedJson(html) {
        const regex = /"chap"\s*:\s*"(\d+(?:\.\d+)?)"(?:[^{}]{0,1200})?"title"\s*:\s*(?:"((?:\\.|[^"\\])*)"|null)/g;
        const volRegex = /"vol"\s*:\s*(?:"(\d+(?:\.\d+)?)"|null)/;
        const entries = [];
        let match;
        let index = 0;

        while ((match = regex.exec(html)) !== null) {
            const chapNum = parseNum(match[1]);
            if (chapNum == null) continue;
            let titleRaw = match[2] ? match[2].replace(/\\"/g, '"').trim() : '';
            const volMatch = volRegex.exec(match[0]);
            const volNum = volMatch ? parseNum(volMatch[1]) : null;
            entries.push({ vol: volNum, chap: chapNum, title: titleRaw, sortKey: index++ });
        }
        return entries;
    }

    function extractComickFromLinks(doc) {
        const capRegex = /(?:(?:Chapter|Capítulo|Ch\.?|Cap\.?|第)\s*)?(\d+(?:\.\d+)?)/i;
        const volRegex = /(?:Vol|Volume)\.?\s*(\d+(?:\.\d+)?)/i;
        const entries = [];
        const seen = new Set();

        doc.querySelectorAll('a[href*="/comic/"][href*="/chapter"], a[href*="/chapter-"]').forEach((link, index) => {
            const href = link.getAttribute('href') || '';
            if (!/\/chapter/i.test(href)) return;

            const linkText = link.textContent.trim();
            const capMatch = capRegex.exec(linkText) || capRegex.exec(href);
            if (!capMatch) return;
            const chapNum = parseNum(capMatch[1]);
            if (chapNum == null || seen.has(chapNum)) return;

            let volNum = null;
            let title = '';
            link.querySelectorAll('span').forEach(span => {
                const spanText = span.textContent.trim();
                const volMatch = volRegex.exec(spanText);
                if (volMatch) volNum = parseNum(volMatch[1]);
                if (span.classList.contains('text-xs') ||
                    (spanText && capRegex.exec(spanText)?.[0] !== spanText && !volRegex.test(spanText))) {
                    if (!title) title = spanText;
                }
            });
            if (!title) {
                const attrTitle = (link.getAttribute('title') || '').trim();
                if (attrTitle.includes(' - ')) title = attrTitle.split(' - ').slice(1).join(' - ').trim();
                else if (attrTitle) title = attrTitle.replace(/^Chapter\s*[\d.]+\s*[-:]?\s*/i, '').trim();
                else title = linkText.replace(/^(?:Chapter|Ch\.?)\s*[\d.]+\s*/i, '').trim();
            }
            if (!cleanImportedTitle(title)) return;

            seen.add(chapNum);
            entries.push({ vol: volNum, chap: chapNum, title, sortKey: index });
        });
        return entries;
    }

    function extractComick(doc) {
        const raw = [];
        let rows = doc.querySelectorAll('table tbody tr.group');
        if (rows.length === 0) rows = doc.querySelectorAll('tr.group');

        rows.forEach((row, index) => {
            const parsed = parseComickRow(row, index);
            if (parsed) raw.push(parsed);
        });

        if (raw.length === 0) {
            raw.push(...extractComickFromLinks(doc));
        }

        if (raw.length === 0) {
            raw.push(...extractComickFromEmbeddedJson(doc.documentElement.innerHTML));
        }

        const nextData = doc.querySelector('script#__NEXT_DATA__');
        if (raw.length === 0 && nextData) {
            raw.push(...extractComickFromEmbeddedJson(nextData.textContent));
        }

        return buildVolumesFromTempCaps(raw);
    }

    function extractComickFan(doc) {
        const volumesMap = {};
        const processed = new Set();
        const titleStripRegex = /^Chapter\s*[\d.]+\s*[-:]?\s*/i;

        doc.querySelectorAll('#chapterList .chapter-items[data-chapter-num], .chapter-items[data-chapter-num]').forEach(item => {
            const chapNum = parseNum(item.getAttribute('data-chapter-num'));
            if (chapNum == null) return;

            const link = item.querySelector('a');
            let title = '';
            const attrTitle = link ? (link.getAttribute('title') || '').trim() : '';
            if (attrTitle.includes(' - ')) title = attrTitle.split(' - ').slice(1).join(' - ').trim();
            else if (attrTitle) title = attrTitle.replace(titleStripRegex, '').trim();

            if (!title) {
                const textEl = item.querySelector('.font-medium font, .font-medium, a');
                const text = textEl ? textEl.textContent.trim() : '';
                title = text.replace(titleStripRegex, '').trim();
            }
            addCapituloSeValido(volumesMap, -1, chapNum, title, true, processed);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaFire
    // -------------------------------------------------------------------------

    function extractMangaFire(doc) {
        const volumesMap = {};
        const volRegex = /(?:Volume|Vol\.?)\s*(\d+(?:\.\d+)?)/i;
        const container = doc.querySelector('ul.scroll-sm');
        if (!container) return [];

        container.querySelectorAll('li.item').forEach(item => {
            const chapNum = parseNum(item.getAttribute('data-number'));
            if (chapNum == null) return;

            let title = '';
            let volNum = -1;
            const link = item.querySelector('a');
            if (link) {
                const firstSpan = link.querySelector('span:first-child');
                const fullText = firstSpan ? firstSpan.textContent.trim() : '';
                const volMatch = volRegex.exec(fullText);
                if (volMatch) volNum = parseNum(volMatch[1]) ?? -1;

                const titleRegex = /^.*?(?:Chapter|Ch\.?|Ch)\s*[\d.]+(?::\s*|\s+)(.*)/i;
                const titleMatch = titleRegex.exec(fullText);
                title = titleMatch ? titleMatch[1].trim() : '';

                if (!title && !/^(?:Chapter|Ch\.?|Ch)\s*[\d.]+$/i.test(fullText)) {
                    title = fullText;
                }
            }
            addChapter(volumesMap, volNum, chapNum, title);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // Taiyo
    // -------------------------------------------------------------------------

    function extractChapterNumber(text) {
        const m = /(?:Capítulo|Chapter)\s*([\d.]+)/i.exec(text);
        return m ? parseNum(m[1]) : null;
    }

    function cleanTaiyoTitle(fullText, chapNum) {
        const esc = String(chapNum).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        let title = fullText.replace(new RegExp(`^(?:Capítulo|Chapter)\\s*${esc}\\s*[—\\-:]\\s*`, 'i'), '');
        if (title === fullText) {
            title = fullText.replace(new RegExp(`^(?:Capítulo|Chapter)\\s*${esc}\\s*`, 'i'), '');
        }
        if (title.trim().toLowerCase() === `capítulo ${chapNum}`.toLowerCase() || !title.trim()) {
            return '';
        }
        return cleanTitle(title.trim());
    }

    function extractTayoChapterFromContainer(container) {
        const p = container.querySelector('p.line-clamp-1');
        if (!p) return null;
        const fullText = p.textContent.trim();
        const chapSpan = p.querySelector('span.font-medium');
        const chapNumStr = chapSpan ? chapSpan.textContent.trim() : null;
        const chapNum = chapNumStr ? parseNum(chapNumStr) : extractChapterNumber(fullText);
        if (chapNum == null) return null;
        return { num: chapNum, title: cleanTaiyoTitle(fullText, String(chapNum)) };
    }

    function extractTayo(doc) {
        const volumesList = [];
        let volumeSections = doc.querySelectorAll('div[data-open="true"]:has(h2 button span.text-foreground)');

        if (volumeSections.length === 0) {
            volumeSections = doc.querySelectorAll('div:has(span[data-open="true"].text-foreground)');
        }

        if (volumeSections.length === 0) {
            const chapters = [];
            doc.querySelectorAll('div.flex.flex-col.gap-1').forEach(container => {
                if (!container.querySelector('p.line-clamp-1')) return;
                const ch = extractTayoChapterFromContainer(container);
                if (ch) chapters.push(ch);
            });
            if (chapters.length > 0) {
                chapters.sort((a, b) => a.num - b.num);
                return [{ volume: -1, chapters }];
            }
            return [];
        }

        volumeSections.forEach(section => {
            const volEl = section.querySelector('h2 button span.text-foreground, span[data-open="true"].text-foreground');
            if (!volEl) return;
            const volText = volEl.textContent.trim().replace(/volume/i, '').trim();
            const volNum = parseNum(volText) ?? 0;

            const chapters = [];
            section.querySelectorAll('section > div.py-2 > div.flex.flex-col.gap-1, div.flex.flex-col.gap-1').forEach(container => {
                if (!container.querySelector('p.line-clamp-1')) return;
                const ch = extractTayoChapterFromContainer(container);
                if (ch) chapters.push(ch);
            });

            if (chapters.length > 0) {
                chapters.sort((a, b) => a.num - b.num);
                volumesList.push({ volume: volNum, chapters });
            }
        });

        volumesList.sort((a, b) => a.volume - b.volume);
        return volumesList;
    }

    // -------------------------------------------------------------------------
    // MangaForest
    // -------------------------------------------------------------------------

    function extractMangaForest(doc) {
        const volumesMap = {};
        const regex = /(?:Vol(?:ume)?\s?(\d+(?:\.\d+)?))?\s?(?:Chapter|Chap|Extra)\s?([\d.]+|Extra(?: V\d+)?)\s?:?\s?(.*)/i;
        const list = doc.getElementById('chapter-list-inner');
        if (!list) return [];

        list.querySelectorAll('li').forEach(li => {
            const strong = li.querySelector('strong');
            if (!strong) return;
            const match = regex.exec(strong.textContent);
            if (!match) return;
            const volNum = parseNum(match[1]) ?? -1;
            const chapterCleaned = match[2].replace(/[^\d.]/g, '');
            const chapNum = parseNum(chapterCleaned) ?? 0;
            addChapter(volumesMap, volNum, chapNum, match[3].trim());
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaRead
    // -------------------------------------------------------------------------

    function extractMangaRead(doc) {
        const volumesMap = {};
        const processed = new Set();
        const chapterList = doc.querySelectorAll('li.wp-manga-chapter');
        const volChapterRegex = /(?:Vol(?:ume)?\s*(\d+(?:\.\d+)?))?\s*(?:Chapter|ch\.|Chap)\s*([\d.]+)\s*:?\s*(.*)/i;

        if (chapterList.length > 0) {
            chapterList.forEach(li => {
                const link = li.querySelector('a');
                if (!link) return;
                const match = volChapterRegex.exec(link.textContent.trim());
                if (!match) return;
                const chapNum = parseNum(match[2]);
                if (chapNum == null || processed.has(chapNum)) return;
                processed.add(chapNum);
                const volNum = parseNum(match[1]) ?? -1;
                addChapter(volumesMap, volNum, chapNum, limparTitulo(match[3].trim()));
            });
        } else {
            const selector = doc.querySelector('div.c-selectpicker.selectpicker_chapter');
            const options = selector ? selector.querySelectorAll('option') : [];
            const regex = /(?:Chapter|ch\.|Chap)\s*([\d.]+)\s*:?\s*(.*)/i;
            const volRegex = /(?:Volume|Vol\.?)\s*(\d+(?:\.\d+)?)/i;

            options.forEach(option => {
                const text = option.textContent.trim();
                const match = regex.exec(text);
                if (!match) return;
                const chapNum = parseNum(match[1]);
                if (chapNum == null || processed.has(chapNum)) return;
                processed.add(chapNum);
                let volNum = -1;
                const volMatch = volRegex.exec(text);
                if (volMatch) volNum = parseNum(volMatch[1]) ?? -1;
                addChapter(volumesMap, volNum, chapNum, limparTitulo(match[2].trim()));
            });
        }

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaK
    // -------------------------------------------------------------------------

    function extractMangaK(doc) {
        const volumesMap = {};
        const processed = new Set();
        const regex = /(?:Vol(?:ume)?\s*(\d+(?:\.\d+)?))?\s*(?:Chapter|ch\.|Chap)\s*([\d.]+)\s*:?\s*(.*)/i;

        doc.querySelectorAll('a[data-chapter-row="true"]').forEach(row => {
            const div13 = row.querySelector('div[class*="text-[13px]"]');
            const divChild = row.querySelector('div > div');
            const linkText = (div13 || divChild) ? (div13 || divChild).textContent : '';
            if (!linkText.trim()) return;

            const match = regex.exec(linkText.trim());
            if (!match) return;
            const chapNum = parseNum(match[2]);
            if (chapNum == null || processed.has(chapNum)) return;
            processed.add(chapNum);
            const volNum = parseNum(match[1]) ?? -1;
            addChapter(volumesMap, volNum, chapNum, limparTitulo(match[3].trim()));
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaKatana
    // -------------------------------------------------------------------------

    function extractMangaKatana(doc) {
        const volumesMap = {};
        const processed = new Set();
        const regex = /(?:Vol(?:ume)?\s*(\d+(?:\.\d+)?))?\s*(?:Chapter|ch\.|Chap)\s*([\d.]+)\s*:?\s*(.*)/i;

        doc.querySelectorAll('div.chapters div.chapter a').forEach(link => {
            const linkText = link.textContent.trim();
            if (!linkText) return;
            const match = regex.exec(linkText);
            if (!match) return;
            const chapNum = parseNum(match[2]);
            if (chapNum == null) return;
            const volNum = parseNum(match[1]) ?? -1;
            addCapituloSeValido(volumesMap, volNum, chapNum, match[3].trim(), false, processed);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaDex
    // -------------------------------------------------------------------------

    function extractMangaDex(doc) {
        const volumesMap = {};
        const processedKeys = new Set();
        const numberRegex = /[\d.]+/;

        doc.querySelectorAll('.chapter-header').forEach(header => {
            const numMatch = numberRegex.exec(header.textContent);
            if (!numMatch) return;
            const chapterNumber = parseNum(numMatch[0]);
            if (chapterNumber == null) return;

            let volumeNumber = -1;
            let parent = header.parentElement;
            while (parent) {
                const volMatch = /Volume\s*([\d.]+)/i.exec(parent.textContent);
                if (volMatch) {
                    volumeNumber = parseNum(volMatch[1]) ?? -1;
                    break;
                }
                parent = parent.parentElement;
            }

            const key = volumeChapterKey(volumeNumber, chapterNumber);
            if (processedKeys.has(key)) return;

            const container = header.parentElement;
            const versions = container ? container.querySelectorAll('.chapter.relative.read') : [];

            let ptTitle = '';
            let enTitle = '';
            let esTitle = '';

            versions.forEach(version => {
                const langImg = version.querySelector('img');
                const descEl = version.querySelector('.line-clamp-1');
                const descText = descEl ? descEl.textContent.trim() : '';
                if (!descText) return;
                const tier = mangadexLangTier(langImg);
                if (tier === 'pt' && !ptTitle) ptTitle = descText;
                else if (tier === 'en' && !enTitle) enTitle = descText;
                else if (tier === 'es' && !esTitle) esTitle = descText;
            });

            const rawTitle = ptTitle || enTitle || esTitle;
            const finalTitle = cleanImportedTitle(rawTitle);
            if (!finalTitle) return;

            processedKeys.add(key);
            if (!volumesMap[volumeNumber]) volumesMap[volumeNumber] = { chapters: [], keepOrder: true };
            volumesMap[volumeNumber].chapters.push({ num: chapterNumber, title: finalTitle });
        });

        return volumesMapToList(volumesMap, true);
    }

    // -------------------------------------------------------------------------
    // MangaPark
    // -------------------------------------------------------------------------

    function extractMangaPark(doc) {
        const volumesMap = {};
        const volRegex = /(?:Volume|Vol\.?)\s*(\d+(?:\.\d+)?)/i;
        const chapterRegex = /(?:Chapter|Ch\.)\s*([\d.]+)/i;

        let items = doc.querySelectorAll('div.tab-content[data-name="chapter"] li.item');
        if (items.length === 0) {
            items = doc.querySelectorAll('li.item[data-number]');
        }

        items.forEach(item => {
            const link = item.querySelector('a');
            if (!link) return;
            const titleAttr = link.getAttribute('title') || '';
            const match = chapterRegex.exec(titleAttr);
            if (!match) return;
            const chapNum = parseNum(match[1]);
            if (chapNum == null) return;

            let volNum = -1;
            const volMatch = volRegex.exec(titleAttr);
            if (volMatch) volNum = parseNum(volMatch[1]) ?? -1;

            let description = '';
            const span = link.querySelector('span');
            if (span) {
                const fullText = span.textContent.trim();
                if (volNum < 0) {
                    const vm = volRegex.exec(fullText);
                    if (vm) volNum = parseNum(vm[1]) ?? -1;
                }
                if (fullText.includes(':')) {
                    description = fullText.substring(fullText.indexOf(':') + 1).trim();
                }
            }

            if (!description) {
                description = titleAttr.replace(match[0], '').trim();
                if (description.startsWith(':') || description.startsWith('-')) {
                    description = description.substring(1).trim();
                }
            }

            addChapter(volumesMap, volNum, chapNum, description);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // ZazaManga
    // -------------------------------------------------------------------------

    function extractZazaManga(doc) {
        const volumesMap = {};
        const processed = new Set();
        const volRegex = /(?:Volume|Vol\.?)\s*(\d+(?:\.\d+)?)/i;
        const chapterRegex = /(?:Chapter|Ch\.?)\s*([\d.]+)/i;
        const hrefChapterRegex = /\/chapter-([\d.]+)(?:\/|$|\?)/i;

        function parseChapterLink(link, text) {
            let chapMatch = chapterRegex.exec(text);
            let chapNum = chapMatch ? parseNum(chapMatch[1]) : null;
            if (chapNum == null) {
                const hrefMatch = hrefChapterRegex.exec(link.getAttribute('href') || '');
                chapNum = hrefMatch ? parseNum(hrefMatch[1]) : null;
            }
            if (chapNum == null) return null;

            let volNum = -1;
            const volMatch = volRegex.exec(text);
            if (volMatch) volNum = parseNum(volMatch[1]) ?? -1;

            let description = '';
            if (text.includes(':')) {
                description = text.substring(text.indexOf(':') + 1).trim();
            } else if (chapMatch) {
                const afterChapter = text.substring(text.indexOf(chapMatch[0]) + chapMatch[0].length).trim();
                if (afterChapter) {
                    description = afterChapter.replace(/^[:\\-]+/, '').trim();
                }
            }

            return { volNum, chapNum, description };
        }

        function processLinks(links) {
            links.forEach(link => {
                const text = link.textContent.trim();
                if (!text) return;
                const parsed = parseChapterLink(link, text);
                if (!parsed) return;
                addCapituloSeValido(volumesMap, parsed.volNum, parsed.chapNum, parsed.description, false, processed);
            });
        }

        let chapterLinks = doc.querySelectorAll('li.wp-manga-chapter a');
        if (chapterLinks.length === 0) {
            chapterLinks = doc.querySelectorAll('#manga-chapters-holder li.wp-manga-chapter a');
        }

        if (chapterLinks.length > 0) {
            processLinks(chapterLinks);
        } else {
            const chapterSelector = doc.querySelector('div.c-selectpicker.selectpicker_chapter, div.selectpicker_chapter');
            const chapterOptions = chapterSelector ? chapterSelector.querySelectorAll('option') : [];
            chapterOptions.forEach(option => {
                const text = option.textContent.trim();
                if (!text) return;
                const chapMatch = chapterRegex.exec(text);
                if (!chapMatch) return;
                const chapNum = parseNum(chapMatch[1]);
                if (chapNum == null) return;
                let volNum = -1;
                const volMatch = volRegex.exec(text);
                if (volMatch) volNum = parseNum(volMatch[1]) ?? -1;
                let description = '';
                if (text.includes(':')) description = text.substring(text.indexOf(':') + 1).trim();
                addCapituloSeValido(volumesMap, volNum, chapNum, description, false, processed);
            });
        }

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaPlanet
    // -------------------------------------------------------------------------

    function cleanEnglishTitle(rawTitle) {
        let title = rawTitle;
        title = title.replace(
            /^(?:CHAPTER\s*[\d.]*\s*:\s*|BONUS CHAPTER\s*:\s*|Special one-shot\s*:\s*|Final Chapter\s*:\s*|CHAPTER\s*[\d.]*\s*-\s*|CHAPTER\s*[\d.]*\s+|CH\.[\d.]* )/i,
            ''
        );
        title = title.replace(/^CHAPTER\s*\d+:/i, '');
        title = title.replace(/^[^A-Za-z0-9]*CHAPTER\s*[\d.]*\s*:?\s*/i, '');
        title = title.replace(/^[^A-Za-z0-9]*CHAPTER\s*[\d.]*\s*:?\s*/i, '');
        return title.trim();
    }

    function parseVolumeNumber(volumeTitle) {
        const m = /Volume\s*(\d+)/i.exec(volumeTitle);
        return m ? parseNum(m[1]) ?? 0 : 0;
    }

    function extractMangaPlanet(doc) {
        const volumesList = [];

        doc.querySelectorAll('div[id^="accordion_"] > div.card.mt-4.select-options').forEach(item => {
            const volTitleEl = item.querySelector('div.card-body.book-detail.panel-collapse h3[id^="vol_title_"]');
            if (!volTitleEl) return;

            const volNum = parseVolumeNumber(volTitleEl.textContent);
            const chapters = [];

            item.querySelectorAll('ul[id^="epi"]').forEach(ul => {
                const chapStr = ul.id.replace(/^epi/, '');
                const chapNum = parseNum(chapStr);
                if (chapNum == null) return;

                const li = ul.querySelector('li.list-group-item');
                if (!li) return;

                const h3 = li.querySelector('h3');
                let ingles = '';
                if (h3) {
                    const p = h3.querySelector('p');
                    ingles = p ? p.textContent.trim() : h3.textContent.trim();
                }
                ingles = cleanEnglishTitle(ingles);

                if (ingles) {
                    chapters.push({ num: chapNum, title: cleanTitle(ingles) });
                }
            });

            if (chapters.length > 0) {
                chapters.sort((a, b) => a.num - b.num);
                volumesList.push({ volume: volNum, chapters });
            }
        });

        return volumesList;
    }

    // -------------------------------------------------------------------------
    // MangaTown
    // -------------------------------------------------------------------------

    function extractMangaTown(doc) {
        const volumesMap = {};
        const processed = new Set();

        const desktopItems = doc.querySelectorAll('ul.chapter_list > li');
        if (desktopItems.length > 0) {
            desktopItems.forEach(li => {
                const link = li.querySelector('a');
                if (!link) return;
                const linkText = link.textContent.trim();
                const chapMatch = MANGATOWN_DESKTOP_CHAPTER_REGEX.exec(linkText);
                if (!chapMatch) return;
                const chapNum = parseNum(chapMatch[1]);
                if (chapNum == null) return;

                let title = '';
                li.querySelectorAll('span').forEach(span => {
                    if (!span.classList.contains('time')) {
                        const spanText = span.textContent.trim();
                        if (spanText) title = spanText;
                    }
                });
                if (!title) {
                    title = linkText.replace(MANGATOWN_DESKTOP_CHAPTER_REGEX, '').trim();
                }
                addCapituloSeValido(volumesMap, -1, chapNum, title, true, processed);
            });
        } else {
            doc.querySelectorAll('ul.detail-ch-list > li').forEach(li => {
                const link = li.querySelector('a');
                if (!link) return;
                const linkText = link.textContent.trim();
                const chapMatch = MANGATOWN_MOBILE_CHAPTER_REGEX.exec(linkText);
                if (!chapMatch) return;
                const chapNum = parseNum(chapMatch[1]);
                if (chapNum == null) return;

                const volSpan = link.querySelector('span.vol');
                const title = volSpan ? volSpan.textContent.trim() : '';
                addCapituloSeValido(volumesMap, -1, chapNum, title, true, processed);
            });
        }

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // MangaHere
    // -------------------------------------------------------------------------

    function extractMangaHere(doc) {
        const volumesMap = {};
        const processed = new Set();

        doc.querySelectorAll('ul.detail-main-list > li').forEach(li => {
            const link = li.querySelector('a');
            if (!link) return;
            const title3 = link.querySelector('p.title3');
            const text = title3 ? title3.textContent.trim() : link.getAttribute('title').trim();
            if (!text) return;
            const parsed = parseChapterFromText(text);
            if (parsed) addParsedChapter(volumesMap, processed, parsed);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // VyManga
    // -------------------------------------------------------------------------

    function extractVyManga(doc) {
        const volumesMap = {};
        const processed = new Set();

        doc.querySelectorAll('a.list-chapter').forEach(link => {
            const span = link.querySelector('span');
            if (!span) return;
            const spanText = span.textContent.trim();
            if (!spanText) return;
            const parsed = parseChapterFromText(spanText);
            if (parsed) addParsedChapter(volumesMap, processed, parsed);
        });

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // KManga
    // -------------------------------------------------------------------------

    function extractKManga(doc) {
        const volumesMap = {};
        const processed = new Set();

        const episodeLinks = doc.querySelectorAll('a.c-episode-item[href*="/episode/"]');
        if (episodeLinks.length > 0) {
            episodeLinks.forEach(link => {
                const ttlEl = link.querySelector('.c-episode-item__ttl');
                const img = link.querySelector('img[alt]');
                const ttl = ttlEl ? ttlEl.textContent.trim() : (img ? img.getAttribute('alt').trim() : '');
                if (!ttl) return;
                const parsed = parseKMangaChapterText(ttl);
                if (parsed) addParsedChapter(volumesMap, processed, parsed, false);
            });
        } else {
            const parts = doc.title.split('|');
            const pageTitle = (parts.length > 1 ? parts.slice(1).join('|') : doc.title).split('/')[0].trim();
            const parsed = parseKMangaChapterText(pageTitle);
            if (parsed) addParsedChapter(volumesMap, processed, parsed, false);
        }

        return volumesMapToList(volumesMap);
    }

    // -------------------------------------------------------------------------
    // Roteamento por site
    // -------------------------------------------------------------------------

    function hostnameMatches(host, fragment) {
        return host.includes(fragment);
    }

    function detectAndExtract(doc) {
        const host = location.hostname.toLowerCase();

        if (hostnameMatches(host, 'comickfan.com')) return extractComickFan(doc);
        if (hostnameMatches(host, 'comick')) return extractComick(doc);
        if (hostnameMatches(host, 'mangaplanet.com')) return extractMangaPlanet(doc);
        if (hostnameMatches(host, 'mangafire')) return extractMangaFire(doc);
        if (hostnameMatches(host, 'taiyo.moe')) return extractTayo(doc);
        if (hostnameMatches(host, 'mangapark')) return extractMangaPark(doc);
        if (hostnameMatches(host, 'zazamanga')) return extractZazaManga(doc);
        if (hostnameMatches(host, 'mangaforest')) return extractMangaForest(doc);
        if (hostnameMatches(host, 'mangaread')) return extractMangaRead(doc);
        if (hostnameMatches(host, 'mangak.io')) return extractMangaK(doc);
        if (hostnameMatches(host, 'mangakatana')) return extractMangaKatana(doc);
        if (hostnameMatches(host, 'mangadex')) return extractMangaDex(doc);
        if (hostnameMatches(host, 'mangatown.com')) return extractMangaTown(doc);
        if (hostnameMatches(host, 'mangahere')) return extractMangaHere(doc);
        if (hostnameMatches(host, 'vymanga.net')) return extractVyManga(doc);
        if (hostnameMatches(host, 'kmanga.kodansha.com')) return extractKManga(doc);

        throw new Error('Site não reconhecido. Verifique se o @match está configurado para este domínio.');
    }

    // -------------------------------------------------------------------------
    // Clipboard e feedback
    // -------------------------------------------------------------------------

    async function copyText(text) {
        try {
            if (typeof GM_setClipboard === 'function') {
                GM_setClipboard(text, 'text');
                return true;
            }
        } catch (_) { /* fallback abaixo */ }

        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch (_) {
            return false;
        }
    }

    function showResultModal(text) {
        const old = document.getElementById('oa-dialogo-capitulos');
        if (old) old.remove();

        const overlay = document.createElement('div');
        overlay.id = 'oa-dialogo-capitulos';
        Object.assign(overlay.style, {
            position: 'fixed', top: '0', left: '0', width: '100%', height: '100%',
            backgroundColor: 'rgba(0,0,0,0.6)', zIndex: '10001',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
        });

        const box = document.createElement('div');
        Object.assign(box.style, {
            backgroundColor: '#fff', padding: '20px', borderRadius: '8px',
            width: '560px', maxWidth: '92%', boxShadow: '0 4px 15px rgba(0,0,0,0.3)',
            display: 'flex', flexDirection: 'column', gap: '10px',
            fontFamily: 'Segoe UI, Roboto, sans-serif', color: '#202124'
        });

        const titulo = document.createElement('h3');
        titulo.textContent = 'Capítulos extraídos';
        titulo.style.margin = '0 0 6px 0';

        const hint = document.createElement('p');
        hint.textContent = 'Cole no popup Capítulos do OrdenarArquivos (menu Colar ou Ctrl+V).';
        hint.style.margin = '0';
        hint.style.fontSize = '13px';
        hint.style.color = '#5f6368';

        const textArea = document.createElement('textarea');
        textArea.value = text;
        Object.assign(textArea.style, {
            width: '100%', height: '240px', padding: '10px',
            border: '1px solid #ccc', borderRadius: '4px', resize: 'vertical',
            fontFamily: 'Consolas, monospace', fontSize: '12px'
        });

        const btnRow = document.createElement('div');
        btnRow.style.display = 'flex';
        btnRow.style.justifyContent = 'flex-end';
        btnRow.style.gap = '10px';

        const btnCopiar = document.createElement('button');
        btnCopiar.textContent = 'Copiar texto';
        Object.assign(btnCopiar.style, {
            padding: '8px 16px', backgroundColor: '#1a73e8', color: 'white',
            border: 'none', borderRadius: '4px', cursor: 'pointer'
        });
        btnCopiar.addEventListener('click', async () => {
            textArea.select();
            const ok = await copyText(text);
            btnCopiar.textContent = ok ? 'Copiado!' : 'Selecione e Ctrl+C';
        });

        const btnFechar = document.createElement('button');
        btnFechar.textContent = 'Fechar';
        Object.assign(btnFechar.style, {
            padding: '8px 16px', backgroundColor: '#e8eaed', color: '#202124',
            border: 'none', borderRadius: '4px', cursor: 'pointer'
        });
        btnFechar.addEventListener('click', () => overlay.remove());

        btnRow.appendChild(btnCopiar);
        btnRow.appendChild(btnFechar);
        box.appendChild(titulo);
        box.appendChild(hint);
        box.appendChild(textArea);
        box.appendChild(btnRow);
        overlay.appendChild(box);
        document.body.appendChild(overlay);
        textArea.focus();
        textArea.select();
    }

    function showToast(message, isError) {
        const id = 'oa-toast-capitulos';
        let toast = document.getElementById(id);
        if (toast) toast.remove();

        toast = document.createElement('div');
        toast.id = id;
        toast.textContent = message;
        Object.assign(toast.style, {
            position: 'fixed', bottom: '24px', right: '24px', zIndex: '10002',
            padding: '12px 18px', borderRadius: '6px', color: '#fff',
            backgroundColor: isError ? '#d93025' : '#188038',
            boxShadow: '0 2px 8px rgba(0,0,0,0.25)',
            fontFamily: 'Segoe UI, Roboto, sans-serif', fontSize: '14px',
            maxWidth: '360px'
        });
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3500);
    }

    // -------------------------------------------------------------------------
    // UI — botão fixo
    // -------------------------------------------------------------------------

    async function onExtrairClick(btn) {
        if (btn.disabled) return;
        btn.disabled = true;
        const originalText = btn.textContent;
        btn.textContent = 'Extraindo...';

        try {
            const volumes = detectAndExtract(document);
            const totalChapters = volumes.reduce((n, v) => n + v.chapters.length, 0);
            if (totalChapters === 0) {
                showToast('Nenhum capítulo encontrado na página.', true);
                return;
            }

            const text = formatVolumesForPaste(volumes);
            const copied = await copyText(text);

            if (copied) {
                showToast(`${totalChapters} capítulo(s) copiado(s) para a área de transferência.`);
            } else {
                showResultModal(text);
                showToast('Abra o diálogo e copie o texto manualmente.', false);
            }
        } catch (err) {
            const msg = err && err.message ? err.message : String(err);
            showToast(msg, true);
            console.error('[OrdenarArquivos Capítulos]', err);
        } finally {
            btn.disabled = false;
            btn.textContent = originalText;
        }
    }

    function criarBotao() {
        if (document.getElementById(BTN_CONTAINER_ID)) return;

        const container = document.createElement('div');
        container.id = BTN_CONTAINER_ID;
        Object.assign(container.style, {
            position: 'fixed',
            zIndex: '9999',
            top: '70px',
            right: '16px'
        });

        const btn = document.createElement('button');
        btn.id = BTN_ID;
        btn.textContent = 'Extrair capítulos';
        Object.assign(btn.style, {
            padding: '9px 14px',
            color: 'white',
            backgroundColor: '#1a73e8',
            border: 'none',
            borderRadius: '5px',
            cursor: 'pointer',
            boxShadow: '0 2px 5px rgba(0,0,0,0.2)',
            fontFamily: 'Segoe UI, Roboto, sans-serif',
            fontSize: '13px',
            fontWeight: '500'
        });
        btn.addEventListener('click', () => onExtrairClick(btn));

        container.appendChild(btn);
        document.body.appendChild(container);
    }

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        criarBotao();
    } else {
        window.addEventListener('load', criarBotao);
    }

    const observer = new MutationObserver(() => {
        if (!document.getElementById(BTN_CONTAINER_ID)) {
            criarBotao();
        }
    });
    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true });
    } else {
        document.addEventListener('DOMContentLoaded', () => {
            observer.observe(document.body, { childList: true, subtree: true });
        });
    }

    // -------------------------------------------------------------------------
    // Autoteste do formatador (executável via console: OA_testFormat())
    // -------------------------------------------------------------------------

    window.OA_testFormat = function OA_testFormat() {
        const sample = [
            { volume: 1, chapters: [{ num: 1, title: 'The Beginning' }, { num: 2, title: 'The End' }] },
            { volume: 2, chapters: [{ num: 3, title: 'More' }] }
        ];
        const expected =
            'Volume 1\nChapter 1: The Beginning\nChapter 2: The End\n\nVolume 2\nChapter 3: More';
        const out = formatVolumesForPaste(sample);
        const ok = out === expected;
        console.log(ok ? 'OA_testFormat: OK' : 'OA_testFormat: FALHOU');
        if (!ok) {
            console.log('Esperado:\n' + expected);
            console.log('Obtido:\n' + out);
        }
        return ok;
    };

    window.OA_testParse = function OA_testParse() {
        const cases = [
            { fn: parseChapterFromText, input: 'Vol.3 Chapter 46 : The Hero\'S Memoirs', expect: { volume: 3, num: 46, title: 'The Hero\'S Memoirs' } },
            { fn: parseChapterFromText, input: 'Capítulo 12: Título em português', expect: { volume: null, num: 12, title: 'Título em português' } },
            { fn: parseChapterFromText, input: 'Ch.040 - Suppression', expect: { volume: null, num: 40, title: 'Suppression' } },
            { fn: parseKMangaChapterText, input: 'CHAPTER142 The Hero Granville Rozzo', expect: { volume: null, num: 142, title: 'The Hero Granville Rozzo' } }
        ];
        let allOk = true;
        for (const c of cases) {
            const result = c.fn(c.input);
            const ok = result && result.num === c.expect.num && result.title === c.expect.title &&
                (c.expect.volume == null ? result.volume == null : result.volume === c.expect.volume);
            if (!ok) {
                console.log('OA_testParse FALHOU:', c.input, result);
                allOk = false;
            }
        }
        console.log(allOk ? 'OA_testParse: OK' : 'OA_testParse: FALHOU');
        return allOk;
    };

    console.info('[OrdenarArquivos] Script de importação de capítulos carregado.');
})();
