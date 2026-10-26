/** Up to two initials: "Mira Okafor" becomes "MO", "alice" becomes "AL". */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0]!.charAt(0) + parts[parts.length - 1]!.charAt(0)).toUpperCase();
  return name.trim().slice(0, 2).toUpperCase();
}

/** Calm, desaturated tints; the same name always gets the same one. Pairs pass AA for the initials. */
const TINTS = [
  "bg-[#e0f2f1] text-[#0f5550] dark:bg-[#12302d] dark:text-[#99e6dc]",
  "bg-[#e8eefc] text-[#26407a] dark:bg-[#18233d] dark:text-[#b4c6f2]",
  "bg-[#f6ece1] text-[#7a4513] dark:bg-[#33240f] dark:text-[#f0c99a]",
  "bg-[#f3e8ef] text-[#7a2c56] dark:bg-[#33182a] dark:text-[#efb8d6]",
  "bg-[#e9f2e3] text-[#365c1e] dark:bg-[#1c2b14] dark:text-[#bfdea8]",
  "bg-[#ecebf0] text-[#3d3a4f] dark:bg-[#24232c] dark:text-[#cfcce0]",
];

export function tintFor(name: string): string {
  let hash = 0;
  for (const char of name.toLowerCase()) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return TINTS[hash % TINTS.length]!;
}
