// ktox_sequence.js — lazy generator-based sequence for ktox-js.
//
// Each KtoxSequence wraps a *generator factory* `() => generator`, where a
// generator is `() => { value, done }` (same shape as an ES iterator).
// Intermediate operations compose factories — no elements are read until a
// terminal operation pulls them.  Short-circuit terminals (any, all, find,
// take, …) stop consuming the upstream the moment they have their answer.
//
// Usage (matches the transpiler output when .asSequence() is used in Kotlin):
//   KtoxSequence.of([1,2,3,4,5])
//     .filter(x => x % 2 === 0)
//     .map(x => x * 10)
//     .toArray();   // [20, 40]

class KtoxSequence {
    /** @param {() => () => {value: *, done: boolean}} genFactory */
    constructor(genFactory) {
        this._genFactory = genFactory;
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /** Wrap an Iterable (Array, Set, Map, …) as a lazy KtoxSequence. */
    static of(source) {
        return new KtoxSequence(() => {
            const iter = source[Symbol.iterator]();
            return () => iter.next();
        });
    }

    /** Infinite sequence of integers starting at `start`, step 1. */
    static generate(start = 0) {
        return new KtoxSequence(() => {
            let n = start;
            return () => ({ value: n++, done: false });
        });
    }

    // -----------------------------------------------------------------------
    // Intermediate operations (return a new KtoxSequence — data not touched)
    // -----------------------------------------------------------------------

    filter(pred) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            return () => {
                let r = gen();
                while (!r.done && !pred(r.value)) r = gen();
                return r;
            };
        });
    }

    filterNot(pred) {
        return this.filter(x => !pred(x));
    }

    filterNotNull() {
        return this.filter(x => x != null);
    }

    filterIndexed(pred) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let i = 0;
            return () => {
                let r = gen();
                while (!r.done && !pred(i++, r.value)) r = gen();
                return r;
            };
        });
    }

    map(transform) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            return () => {
                const r = gen();
                if (r.done) return r;
                return { value: transform(r.value), done: false };
            };
        });
    }

    mapIndexed(transform) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let i = 0;
            return () => {
                const r = gen();
                if (r.done) return r;
                return { value: transform(i++, r.value), done: false };
            };
        });
    }

    mapNotNull(transform) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            return () => {
                let r = gen();
                while (!r.done) {
                    const v = transform(r.value);
                    if (v != null) return { value: v, done: false };
                    r = gen();
                }
                return r;
            };
        });
    }

    flatMap(transform) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let inner = null;
            return () => {
                while (true) {
                    if (inner) {
                        const r = inner.next();
                        if (!r.done) return r;
                        inner = null;
                    }
                    const r = gen();
                    if (r.done) return r;
                    const sub = transform(r.value);
                    // Obtain a JS iterator from the sub-result: any iterable (Array,
                    // KtoxSequence via Symbol.iterator, etc.) is supported.
                    if (sub != null && sub[Symbol.iterator]) {
                        inner = sub[Symbol.iterator]();
                    } else {
                        // Treat a non-iterable as a single-element "sequence"
                        inner = [sub][Symbol.iterator]();
                    }
                }
            };
        });
    }

    flatten() {
        return this.flatMap(x => x);
    }

    take(n) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let remaining = n;
            return () => {
                if (remaining <= 0) return { value: undefined, done: true };
                remaining--;
                return gen();
            };
        });
    }

    takeWhile(pred) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let done = false;
            return () => {
                if (done) return { value: undefined, done: true };
                const r = gen();
                if (r.done || !pred(r.value)) { done = true; return { value: undefined, done: true }; }
                return r;
            };
        });
    }

    drop(n) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let toDrop = n;
            return () => {
                while (toDrop > 0) { toDrop--; const r = gen(); if (r.done) return r; }
                return gen();
            };
        });
    }

    dropWhile(pred) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let dropping = true;
            return () => {
                while (dropping) {
                    const r = gen();
                    if (r.done) return r;
                    if (!pred(r.value)) { dropping = false; return r; }
                }
                return gen();
            };
        });
    }

    distinct() {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            const seen = new Set();
            return () => {
                let r = gen();
                while (!r.done) {
                    if (!seen.has(r.value)) { seen.add(r.value); return r; }
                    r = gen();
                }
                return r;
            };
        });
    }

    distinctBy(keySelector) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            const seen = new Set();
            return () => {
                let r = gen();
                while (!r.done) {
                    const k = keySelector(r.value);
                    if (!seen.has(k)) { seen.add(k); return r; }
                    r = gen();
                }
                return r;
            };
        });
    }

    sorted() {
        return KtoxSequence.of(this.toArray().slice().sort((a, b) => a < b ? -1 : a > b ? 1 : 0));
    }

    sortedDescending() {
        return KtoxSequence.of(this.toArray().slice().sort((a, b) => a > b ? -1 : a < b ? 1 : 0));
    }

    sortedBy(keySelector) {
        return KtoxSequence.of(this.toArray().slice().sort((a, b) => {
            const ka = keySelector(a), kb = keySelector(b);
            return ka < kb ? -1 : ka > kb ? 1 : 0;
        }));
    }

    sortedByDescending(keySelector) {
        return KtoxSequence.of(this.toArray().slice().sort((a, b) => {
            const ka = keySelector(a), kb = keySelector(b);
            return ka > kb ? -1 : ka < kb ? 1 : 0;
        }));
    }

    sortedWith(comparator) {
        return KtoxSequence.of(this.toArray().slice().sort(comparator));
    }

    reversed() {
        return KtoxSequence.of(this.toArray().reverse());
    }

    onEach(action) {
        return this.map(x => { action(x); return x; });
    }

    onEachIndexed(action) {
        return this.mapIndexed((i, x) => { action(i, x); return x; });
    }

    chunked(size) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let exhausted = false;
            return () => {
                if (exhausted) return { value: undefined, done: true };
                const chunk = [];
                for (let i = 0; i < size; i++) {
                    const r = gen();
                    if (r.done) { exhausted = true; break; }
                    chunk.push(r.value);
                }
                if (chunk.length === 0) return { value: undefined, done: true };
                return { value: chunk, done: false };
            };
        });
    }

    windowed(size, step = 1, partialWindows = false) {
        const arr = this.toArray();
        const result = [];
        for (let i = 0; i < arr.length; i += step) {
            const w = arr.slice(i, i + size);
            if (w.length === size || partialWindows) result.push(w);
        }
        return KtoxSequence.of(result);
    }

    zipWithNext(transform) {
        const arr = this.toArray();
        const result = [];
        for (let i = 0; i < arr.length - 1; i++) {
            result.push(transform ? transform(arr[i], arr[i + 1]) : [arr[i], arr[i + 1]]);
        }
        return KtoxSequence.of(result);
    }

    zip(other, transform) {
        const arr = this.toArray();
        const otherArr = Array.isArray(other) ? other : (other instanceof KtoxSequence ? other.toArray() : [...other]);
        const result = [];
        const len = Math.min(arr.length, otherArr.length);
        for (let i = 0; i < len; i++) {
            result.push(transform ? transform(arr[i], otherArr[i]) : [arr[i], otherArr[i]]);
        }
        return KtoxSequence.of(result);
    }

    withIndex() {
        return this.mapIndexed((i, v) => ({ index: i, value: v }));
    }

    runningFold(initial, operation) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let acc = initial;
            let emittedInitial = false;
            return () => {
                if (!emittedInitial) { emittedInitial = true; return { value: acc, done: false }; }
                const r = gen();
                if (r.done) return r;
                acc = operation(acc, r.value);
                return { value: acc, done: false };
            };
        });
    }

    scan(initial, operation) {
        return this.runningFold(initial, operation);
    }

    runningReduce(operation) {
        const upstream = this._genFactory;
        return new KtoxSequence(() => {
            const gen = upstream();
            let acc = undefined;
            let started = false;
            return () => {
                if (!started) {
                    const r = gen();
                    if (r.done) return r;
                    acc = r.value;
                    started = true;
                    return { value: acc, done: false };
                }
                const r = gen();
                if (r.done) return r;
                acc = operation(acc, r.value);
                return { value: acc, done: false };
            };
        });
    }

    plus(other) {
        const self = this;
        return new KtoxSequence(() => {
            const gen1 = self._genFactory();
            const otherArr = Array.isArray(other) ? other : (other instanceof KtoxSequence ? other.toArray() : [other]);
            const gen2 = KtoxSequence.of(otherArr)._genFactory();
            let first = true;
            return () => {
                if (first) {
                    const r = gen1();
                    if (!r.done) return r;
                    first = false;
                }
                return gen2();
            };
        });
    }

    minus(element) {
        let removed = false;
        return this.filter(x => {
            if (!removed && x === element) { removed = true; return false; }
            return true;
        });
    }

    asSequence() { return this; }

    asIterable() { return this.toArray(); }

    constrainOnce() { return this; }

    // -----------------------------------------------------------------------
    // Terminal operations
    // -----------------------------------------------------------------------

    /** Pull all elements into a JavaScript Array. */
    toArray() {
        const result = [];
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { result.push(r.value); r = gen(); }
        return result;
    }

    /** Alias for toArray() — matches Kotlin's toList(). */
    toList() { return this.toArray(); }
    toMutableList() { return this.toArray(); }
    toSet() { return new Set(this.toArray()); }
    toMutableSet() { return new Set(this.toArray()); }

    forEach(action) { const arr = this.toArray(); for (const x of arr) action(x); }
    forEachIndexed(action) { const arr = this.toArray(); arr.forEach((v, i) => action(i, v)); }

    fold(initial, operation) {
        let acc = initial;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { acc = operation(acc, r.value); r = gen(); }
        return acc;
    }

    reduce(operation) {
        const gen = this._genFactory();
        let r = gen();
        if (r.done) throw new Error("Sequence is empty.");
        let acc = r.value;
        r = gen();
        while (!r.done) { acc = operation(acc, r.value); r = gen(); }
        return acc;
    }

    reduceOrNull(operation) {
        try { return this.reduce(operation); } catch { return null; }
    }

    any(pred) {
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (pred(r.value)) return true; r = gen(); }
        return false;
    }
    /** Alias so native Array method names work on KtoxSequence too. */
    some(pred) { return this.any(pred); }

    all(pred) {
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (!pred(r.value)) return false; r = gen(); }
        return true;
    }
    every(pred) { return this.all(pred); }

    none(pred) {
        if (!pred) {
            const gen = this._genFactory();
            return gen().done;
        }
        return !this.any(pred);
    }

    count(pred) {
        let n = 0;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (!pred || pred(r.value)) n++; r = gen(); }
        return n;
    }

    get length() { return this.count(); }

    sum() { return this.fold(0, (a, b) => a + b); }
    sumOf(selector) { return this.fold(0, (a, b) => a + selector(b)); }

    average() {
        let sum = 0, n = 0;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { sum += r.value; n++; r = gen(); }
        return n === 0 ? NaN : sum / n;
    }

    min() {
        let m = undefined;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (m === undefined || r.value < m) m = r.value; r = gen(); }
        return m ?? null;
    }
    minOrNull() { return this.min(); }

    max() {
        let m = undefined;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (m === undefined || r.value > m) m = r.value; r = gen(); }
        return m ?? null;
    }
    maxOrNull() { return this.max(); }

    minBy(selector) {
        let best = undefined, bestKey = undefined;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            const k = selector(r.value);
            if (bestKey === undefined || k < bestKey) { best = r.value; bestKey = k; }
            r = gen();
        }
        return best ?? null;
    }
    minByOrNull(s) { return this.minBy(s); }

    maxBy(selector) {
        let best = undefined, bestKey = undefined;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            const k = selector(r.value);
            if (bestKey === undefined || k > bestKey) { best = r.value; bestKey = k; }
            r = gen();
        }
        return best ?? null;
    }
    maxByOrNull(s) { return this.maxBy(s); }

    find(pred) {
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (pred(r.value)) return r.value; r = gen(); }
        return null;
    }
    findLast(pred) {
        let last = null;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (pred(r.value)) last = r.value; r = gen(); }
        return last;
    }

    first(pred) {
        if (!pred) {
            const r = this._genFactory()();
            if (r.done) throw new Error("Sequence is empty.");
            return r.value;
        }
        const v = this.find(pred);
        if (v === null) throw new Error("No element matching predicate.");
        return v;
    }
    firstOrNull(pred) {
        if (!pred) { const r = this._genFactory()(); return r.done ? null : r.value; }
        return this.find(pred);
    }

    last(pred) {
        const arr = this.toArray();
        if (!pred) {
            if (arr.length === 0) throw new Error("Sequence is empty.");
            return arr[arr.length - 1];
        }
        for (let i = arr.length - 1; i >= 0; i--) if (pred(arr[i])) return arr[i];
        throw new Error("No element matching predicate.");
    }
    lastOrNull(pred) {
        try { return this.last(pred); } catch { return null; }
    }

    single(pred) {
        const arr = pred ? this.filter(pred).toArray() : this.toArray();
        if (arr.length !== 1) throw new Error("Sequence does not contain exactly one element.");
        return arr[0];
    }
    singleOrNull(pred) {
        try { return this.single(pred); } catch { return null; }
    }

    elementAt(index) {
        let i = 0;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (i === index) return r.value; i++; r = gen(); }
        throw new Error(`Index ${index} out of bounds.`);
    }
    elementAtOrNull(index) { try { return this.elementAt(index); } catch { return null; } }
    elementAtOrElse(index, defaultFn) { try { return this.elementAt(index); } catch { return defaultFn(index); } }

    indexOf(element) {
        let i = 0;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (r.value === element) return i; i++; r = gen(); }
        return -1;
    }
    lastIndexOf(element) {
        let i = 0, last = -1;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (r.value === element) last = i; i++; r = gen(); }
        return last;
    }
    indexOfFirst(pred) {
        let i = 0;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (pred(r.value)) return i; i++; r = gen(); }
        return -1;
    }
    indexOfLast(pred) {
        let i = 0, last = -1;
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (pred(r.value)) last = i; i++; r = gen(); }
        return last;
    }

    contains(element) {
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) { if (r.value === element) return true; r = gen(); }
        return false;
    }

    groupBy(keySelector, valueTransform) {
        const map = new Map();
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            const k = keySelector(r.value);
            const v = valueTransform ? valueTransform(r.value) : r.value;
            if (!map.has(k)) map.set(k, []);
            map.get(k).push(v);
            r = gen();
        }
        return map;
    }

    associate(transform) {
        const map = new Map();
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            const [k, v] = transform(r.value);
            map.set(k, v);
            r = gen();
        }
        return map;
    }

    associateBy(keySelector, valueTransform) {
        const map = new Map();
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            const k = keySelector(r.value);
            const v = valueTransform ? valueTransform(r.value) : r.value;
            map.set(k, v);
            r = gen();
        }
        return map;
    }

    associateWith(valueSelector) {
        return this.associateBy(x => x, valueSelector);
    }

    joinToString(separator = ', ', transform) {
        const parts = this.toArray().map(x => transform ? transform(x) : String(x));
        return parts.join(separator);
    }

    partition(pred) {
        const yes = [], no = [];
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            (pred(r.value) ? yes : no).push(r.value);
            r = gen();
        }
        return [yes, no];
    }

    unzip() {
        const firsts = [], seconds = [];
        const gen = this._genFactory();
        let r = gen();
        while (!r.done) {
            firsts.push(r.value[0]);
            seconds.push(r.value[1]);
            r = gen();
        }
        return [firsts, seconds];
    }

    shuffled() {
        const arr = this.toArray();
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
        }
        return KtoxSequence.of(arr);
    }

    ifEmpty(defaultFn) {
        const arr = this.toArray();
        return arr.length === 0 ? KtoxSequence.of(defaultFn()) : KtoxSequence.of(arr);
    }

    requireNoNulls() {
        return this.map(x => {
            if (x == null) throw new Error("Null element found in sequence.");
            return x;
        });
    }

    // -----------------------------------------------------------------------
    // Symbol.iterator — makes KtoxSequence usable in for…of loops
    // -----------------------------------------------------------------------

    [Symbol.iterator]() {
        const gen = this._genFactory();
        return {
            next() { return gen(); }
        };
    }
}

// CommonJS + ESModule dual export
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { KtoxSequence };
} else if (typeof exports !== 'undefined') {
    exports.KtoxSequence = KtoxSequence;
}
