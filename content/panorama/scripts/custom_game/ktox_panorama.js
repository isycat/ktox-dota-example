// ktox_panorama.js — Panorama UI panel binding helpers for ktox-panorama.
//
// Provides null-safe, idiomatic helpers for binding Panorama panel children at
// runtime.  These helpers are deployed alongside transpiled code in all build
// modes (static, live dev, and ktox-dota).
//
// Usage:
//   var label  = ktoxFindChild(parent, "Label", "HeroName");
//   var items  = ktoxFindChildren(parent, "Panel", null, "ability-slot");
//   ktoxGraftClass(MyPanelClass, panel);

/**
 * Compute a class-match score for a panel against a space-separated class selector.
 *
 * cls may contain multiple space-separated tokens (e.g. "button bg-green py-4").
 * Each token is tested individually via BHasClass so that a panel still matches
 * even when some classes have been dynamically added or removed at runtime.
 *
 * Scoring rules:
 *  - Full match (every non-empty token present): returns Infinity so that panels
 *    with all specified classes always outrank any partial match.
 *  - Partial match: weighted sum where the i-th token (0-based) contributes
 *    (n - i) points when present; earlier-listed tokens therefore carry more
 *    weight, reflecting that the caller considers them more identifying.
 *  - No match (no token found): returns 0.
 *
 * A score of 0 means the class selector should be treated as unapplicable for
 * this candidate (preserved for resilience when all classes are absent).
 *
 * @param {Panel}  panel  The panel to score.
 * @param {string} cls    Space-separated list of CSS classes.
 * @returns {number}      Infinity for a full match, >0 for partial, 0 for none.
 */
function ktoxClassScore(panel, cls) {
    if (!panel.BHasClass) return 0;
    // Fast path: single token (most common case — avoids split() allocation).
    if (cls.indexOf(' ') === -1) return panel.BHasClass(cls) ? Infinity : 0;
    var tokens = cls.split(' ');
    var n = tokens.length;
    var total = 0;
    var matched = 0;
    var score = 0;
    for (var i = 0; i < n; i++) {
        var t = tokens[i];
        if (!t) continue;
        total++;
        if (panel.BHasClass(t)) { matched++; score += (n - i); }
    }
    return (matched === total && total > 0) ? Infinity : score;
}

/**
 * Filter candidates by class using ktoxClassScore, keeping only those with the
 * highest score.  If no candidate scores above 0 the original array is returned
 * unchanged, preserving resilience when all specified classes are absent at the
 * time of the call.
 *
 * @param {Panel[]} candidates  Current candidate panels.
 * @param {string}  cls         Space-separated class selector.
 * @returns {Panel[]}           Best-scoring subset, or the original array.
 */
function ktoxFilterByClass(candidates, cls) {
    var best = 0;
    var result = [];
    for (var i = 0; i < candidates.length; i++) {
        var s = ktoxClassScore(candidates[i], cls);
        if (s > best) { best = s; result = [candidates[i]]; }
        else if (s > 0 && s === best) { result.push(candidates[i]); }
    }
    return best === 0 ? candidates : result;
}

/**
 * Find a single child panel matching the given filters.
 *
 * Filters are applied as a cascade: tag is always required, then id and cls
 * are applied in order only when the current candidate set still has more than
 * one match.  This means a reliable tag+id pair will win without ever needing
 * the class, while cls acts as a tie-breaker for panels whose classes may be
 * dynamically added or removed.  If a filter step would eliminate all
 * remaining candidates it is skipped to preserve resilience.
 *
 * cls may be a space-separated list of tokens (e.g. "button secondary disabled").
 * Tokens are matched individually via BHasClass and weighted by position so that
 * earlier-listed classes are considered more identifying.  A full match (all
 * tokens present) always outranks any partial match.
 *
 * Two index parameters are supported:
 *  - index      Scoped index: zero-based position within the fully-filtered
 *               (tag + id + cls) candidate set.  This is the primary selector.
 *  - tagIndex   Tag index: zero-based position within the tag-only set, used as
 *               the final fallback when the scoped index yields no result.  id
 *               and cls are intentionally ignored so the fallback is the nth
 *               panel of the given type regardless of its id or classes.
 *
 * Returns null when parent is null/undefined so callers can safely chain calls
 * without extra null guards.
 *
 * @param {Panel|null|undefined} parent  The parent panel to search.
 * @param {string} tag                   Required panel type (e.g. "Label", "Panel", "Image").
 * @param {string|null} [id]             Optional panel id for disambiguation (exact match).
 * @param {string|null} [cls]            Optional CSS classes for disambiguation (weighted partial match).
 * @param {number} [index=0]             Zero-based scoped index into the fully-filtered candidate set.
 * @param {number} [tagIndex]            Zero-based index into the tag-only set (fallback, ignores id/cls).
 * @returns {Panel|null}                 Matching child, or null.
 */
function ktoxFindChild(parent, tag, id, cls, index, tagIndex) {
    if (parent == null) return null;
    if (index === undefined) index = 0;
    var children = parent.Children();
    var n = children.length;
    // byTag is only needed when tagIndex is provided and the primary lookup fails.
    var byTag = tagIndex !== undefined ? [] : null;
    var candidates = [];
    for (var i = 0; i < n; i++) {
        var c = children[i];
        if (c.paneltype === tag) {
            candidates.push(c);
            if (byTag !== null) byTag.push(c);
        }
    }
    if (candidates.length > 1) {
        if (id != null) {
            var byId = [];
            for (var i = 0; i < candidates.length; i++) {
                if (candidates[i].id === id) byId.push(candidates[i]);
            }
            if (byId.length > 0) candidates = byId;
        }
        if (candidates.length > 1 && cls != null) {
            candidates = ktoxFilterByClass(candidates, cls);
        }
    }
    if (candidates.length > index) return candidates[index];
    if (byTag !== null && byTag.length > tagIndex) return byTag[tagIndex];
    return null;
}

/**
 * Find all child panels matching the given filters.
 *
 * Filters are applied as a cascade: tag is always required, then id and cls
 * are applied in order only when the current candidate set still has more than
 * one match.  If a filter step would eliminate all remaining candidates it is
 * skipped to preserve resilience (cls in particular may be absent due to
 * dynamic class changes).
 *
 * cls may be a space-separated list of tokens (e.g. "button secondary disabled").
 * Tokens are matched individually via BHasClass and weighted by position so that
 * earlier-listed classes are considered more identifying.  A full match (all
 * tokens present) always outranks any partial match.
 *
 * Always returns an array (never null); returns [] when parent is null/undefined
 * or when no children match.
 *
 * @param {Panel|null|undefined} parent  The parent panel to search.
 * @param {string} tag                   Required panel type (e.g. "Label", "Panel", "Image").
 * @param {string|null} [id]             Optional panel id for disambiguation (exact match).
 * @param {string|null} [cls]            Optional CSS classes for disambiguation (weighted partial match).
 * @returns {Panel[]}                    Filtered array of matching children (may be empty).
 */
function ktoxFindChildren(parent, tag, id, cls) {
    if (parent == null) return [];
    var children = parent.Children();
    var n = children.length;
    var candidates = [];
    for (var i = 0; i < n; i++) {
        var c = children[i];
        if (c.paneltype === tag) candidates.push(c);
    }
    if (candidates.length > 1) {
        if (id != null) {
            var byId = [];
            for (var i = 0; i < candidates.length; i++) {
                if (candidates[i].id === id) byId.push(candidates[i]);
            }
            if (byId.length > 0) candidates = byId;
        }
        if (candidates.length > 1 && cls != null) {
            candidates = ktoxFilterByClass(candidates, cls);
        }
    }
    return candidates;
}

/**
 * Graft a class onto a Panorama panel's prototype chain.
 *
 * Walks to the deepest prototype of clazz.prototype (stopping before
 * Object.prototype or the panel's own proto) and sets it to
 * Object.getPrototypeOf(panel), so the class hierarchy gains access to
 * the panel's native Panorama API.
 *
 * Replaces the inline IIFE pattern emitted by older transpiler output:
 *   (function(clazz, oldProto) { ... })(MyClass, Object.getPrototypeOf(panel));
 *
 * @param {Function} clazz  The class to graft.
 * @param {Panel}    panel  The panel whose prototype chain to graft onto.
 */
function ktoxGraftClass(clazz, panel) {
    var oldProto = Object.getPrototypeOf(panel);
    var p = clazz.prototype;
    while (true) {
        var next = Object.getPrototypeOf(p);
        if (!next || next === Object.prototype || next === oldProto) break;
        p = next;
    }
    if (Object.getPrototypeOf(p) !== oldProto) Object.setPrototypeOf(p, oldProto);
}
