// package: com.isycat.ktox.dota.lib.panorama

import { DollarStatic } from "./com/isycat/dota/types/panorama/DollarStatic.js";
import { Panel } from "./com/isycat/dota/types/panorama/Panel.js";

function Panel_N_ext_matchesSegment(self, segId, segClasses, segTag) {
    if (segId !== null && self.id !== segId) {
        return false;
    }
    if (segTag !== null && self.paneltype !== segTag) {
        return false;
    }
    for (const cls of segClasses) {
        if (!self.hasClass(cls)) {
            return false;
        }
    }
    return true;
}

function Panel_N_ext_collectMatchingChildren(self, segId, segClasses, segTag, deep, result) {
    const count = self.childCount;
    for (let i = 0; i < count; i++) {
        const child = self.getChild(i);
        if (child !== null) {
            if (Panel_N_ext_matchesSegment(child, segId, segClasses, segTag)) {
                result.push(child);
            }
            if (deep) {
                Panel_N_ext_collectMatchingChildren(child, segId, segClasses, segTag, true, result);
            }
        }
    }
}

function applySegment(sources, segment, deep) {
    let rest = segment;
    let segId = null;
    let segTag = null;
    const segClasses = [];
    const hashIdx = rest.indexOf('#');
    if (hashIdx >= 0) {
        segId = rest.substring(hashIdx + 1);
        rest = rest.substring(0, hashIdx);
    }
    if (rest.length > 0) {
        if (rest.startsWith('.')) {
            const parts = rest.split('.');
            for (let k = 1; k < parts.length; k++) {
                const cls = parts[k];
                if (cls.length > 0) {
                    segClasses.push(cls);
                }
            }
        }
        else {
            const dotIdx = rest.indexOf('.');
            if (dotIdx < 0) {
                segTag = rest;
            }
            else {
                segTag = rest.substring(0, dotIdx);
                const classParts = rest.substring(dotIdx + 1).split('.');
                for (const cls of classParts) {
                    if (cls.length > 0) {
                        segClasses.push(cls);
                    }
                }
            }
        }
    }
    const result = [];
    for (const panel of sources) {
        Panel_N_ext_collectMatchingChildren(panel, segId, segClasses, segTag, deep, result);
    }
    return result;
}

function Panel_N_ext_invoke(self, selector) {
    const tokens = selector.trim().replace(" > ", " >").replace("> ", ">").replace(" >", " >").split(' ');
    let current = [self];
    current.push(self);
    let pendingDirect = false;
    for (const rawToken of tokens) {
        const token = rawToken.trim();
        if (token.length === 0) {
            continue;
        }
        if (token === ">") {
            pendingDirect = true;
            continue;
        }
        const isDirect = token.startsWith('>') || pendingDirect;
        const seg = (token.startsWith('>') ? token.substring(1) : token);
        pendingDirect = false;
        if (seg.length === 0) {
            continue;
        }
        const next = applySegment(current, seg, !isDirect);
        current = [];
        current.push(...next);
    }
    return current;
}

