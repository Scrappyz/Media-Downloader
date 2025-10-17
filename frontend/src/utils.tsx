// parseFilenameFromContentDisposition.ts
export function parseFilenameFromContentDisposition(header?: string | null): string | null {
  if (!header) return null;

  // 1) Try RFC 5987: filename*=charset'lang'%encoded
  const filenameStarMatch = header.match(/filename\*\s*=\s*([^;]+)/i);
  if (filenameStarMatch) {
    const raw = filenameStarMatch[1].trim();
    // raw is like: UTF-8''Some%20Name.mp4 or iso-8859-1'en'%A3rates.txt
    const parts = raw.split("''");
    if (parts.length === 2) {
      try {
        // parts[0] might be "UTF-8" or "utf-8"
        // const charset = parts[0].toLowerCase(); // note: we don't use it for decoding in browser (decodeURIComponent assumes UTF-8)
        const encoded = parts[1];
        // decode percent-encoding (assumes UTF-8 per modern servers; if charset != utf-8, decodeURIComponent may mis-decode)
        return decodeURIComponent(encoded);
      } catch (e) {
        // fall through to other strategies
      }
    } else {
      // Sometimes servers omit charset/lang and send percent-encoded literal; try decodeURIComponent directly
      try {
        return decodeURIComponent(raw);
      } catch (_) {}
    }
  }

  // 2) Try quoted filename: filename="value"
  const quotedMatch = header.match(/filename\s*=\s*"([^"]+)"/i);
  if (quotedMatch && quotedMatch[1]) {
    // unescape quoted-pairs (\" and \\)
    return quotedMatch[1].replace(/\\(["\\])/g, "$1");
  }

  // 3) Try unquoted filename: filename=value
  const unquotedMatch = header.match(/filename\s*=\s*([^;]+)/i);
  if (unquotedMatch && unquotedMatch[1]) {
    const raw = unquotedMatch[1].trim();
    return raw.replace(/(^'|'$)/g, ""); // trim stray single quotes
  }

  return null;
}
