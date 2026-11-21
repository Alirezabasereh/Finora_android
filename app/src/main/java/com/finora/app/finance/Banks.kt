package com.finora.app.finance

/** Iranian bank logos — detected from the account NAME (nothing extra is stored), exactly like the web app. */
object Banks {
    data class Bank(val id: String, val name: String, val any: List<String>, val all: List<List<String>> = emptyList())

    val list: List<Bank> = listOf(
        Bank("ansar", "بانک انصار", listOf("انصار", "ansar")),
        Bank("ayandeh", "بانک آینده", listOf("اینده", "ayandeh", "ayande")),
        Bank("blu", "بلوبانک", listOf("بلو", "blu")),
        Bank("day", "بانک دی", listOf("دی", "day")),
        Bank("eghtesadnovin", "بانک اقتصاد نوین", listOf("اقتصادنوین", "eghtesadnovin"), listOf(listOf("اقتصاد", "نوین"), listOf("eghtesad", "novin"))),
        Bank("gardeshgari", "بانک گردشگری", listOf("گردشگری", "gardeshgari")),
        Bank("ghavamin", "بانک قوامین", listOf("قوامین", "ghavamin", "qavamin")),
        Bank("hekmat", "بانک حکمت", listOf("حکمت", "hekmat")),
        Bank("iranzamin", "بانک ایران زمین", listOf("ایرانزمین", "iranzamin"), listOf(listOf("ایران", "زمین"), listOf("iran", "zamin"))),
        Bank("karafarin", "بانک کارآفرین", listOf("کارافرین", "karafarin"), listOf(listOf("کار", "افرین"))),
        Bank("keshavarzi", "بانک کشاورزی", listOf("کشاورزی", "keshavarzi")),
        Bank("khavarmianeh", "بانک خاورمیانه", listOf("خاورمیانه", "khavarmianeh", "khavarmiyaneh"), listOf(listOf("خاور", "میانه"), listOf("khavar", "mianeh"), listOf("middle", "east"))),
        Bank("maskan", "بانک مسکن", listOf("مسکن", "maskan")),
        Bank("mehr", "بانک مهر ایران", listOf("مهر", "mehr")),
        Bank("melal", "موسسه اعتباری ملل", listOf("ملل", "melal")),
        Bank("mellat", "بانک ملت", listOf("ملت", "mellat")),
        Bank("melli", "بانک ملی ایران", listOf("ملی", "melli")),
        Bank("noor", "موسسه اعتباری نور", listOf("نور", "noor", "nour")),
        Bank("parsian", "بانک پارسیان", listOf("پارسیان", "parsian")),
        Bank("pasargad", "بانک پاسارگاد", listOf("پاسارگاد", "pasargad")),
        Bank("post", "پست بانک ایران", listOf("پست", "post")),
        Bank("refah", "بانک رفاه کارگران", listOf("رفاه", "refah")),
        Bank("resalat", "بانک قرض‌الحسنه رسالت", listOf("رسالت", "resalat")),
        Bank("saderat", "بانک صادرات ایران", listOf("صادرات", "saderat")),
        Bank("saman", "بانک سامان", listOf("سامان", "saman")),
        Bank("sarmayeh", "بانک سرمایه", listOf("سرمایه", "sarmayeh", "sarmaye")),
        Bank("sepah", "بانک سپه", listOf("سپه", "sepah")),
        Bank("shahr", "بانک شهر", listOf("شهر", "shahr")),
        Bank("sina", "بانک سینا", listOf("سینا", "sina")),
        Bank("tejarat", "بانک تجارت", listOf("تجارت", "tejarat")),
        Bank("tosee", "بانک توسعه تعاون", listOf("توسعه", "tosee", "toseeh", "tosea"))
    )

    private val nonWord = Regex("[^a-z0-9\\u0600-\\u06FF]+")
    private val diacritics = Regex("[\\u064B-\\u065F\\u0670\\u0640]")

    fun normalize(str: String?): String =
        (str ?: "").lowercase()
            .replace(Regex("[يى]"), "ی").replace("ك", "ک").replace(Regex("[ۀة]"), "ه")
            .replace(Regex("[آأإٱ]"), "ا")
            .replace(diacritics, "")
            .replace(nonWord, " ")
            .trim()

    private fun words(str: String?): List<String> {
        val out = ArrayList<String>()
        for (w in normalize(str).split(' ').filter { it.isNotEmpty() }) {
            out.add(w)
            val t = w.replace(Regex("^(بانک|bank)"), "").replace(Regex("(بانک|bank)$"), "")
            if (t.isNotEmpty() && t != w) out.add(t)
        }
        return out
    }

    /** The bank whose keyword appears first in the text, or null. */
    fun detect(text: String?): Bank? {
        val ws = words(text)
        if (ws.isEmpty()) return null
        var best: Bank? = null
        var bestPos = Int.MAX_VALUE
        for (bank in list) {
            var pos = Int.MAX_VALUE
            for (k in bank.any) { val i = ws.indexOf(k); if (i != -1 && i < pos) pos = i }
            for (group in bank.all) {
                val idx = group.map { ws.indexOf(it) }
                if (idx.all { it != -1 }) pos = minOf(pos, idx.min())
            }
            if (pos < bestPos) { bestPos = pos; best = bank }
        }
        return best
    }
}
