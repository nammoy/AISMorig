package com.app.aisheildme.bytrix.data.ai

object CheckInPrompts {

    fun scanSummary(scannedText: String): String {
        val body = scannedText.take(OnDeviceAi.MAX_PROMPT_CHARS - 700)
        return """
You are a clinical intake assistant summarising a scanned patient check-in form.

Rules:
- Use ONLY facts that appear in the FORM TEXT. Never invent or estimate a value.
- If a section has no information, write "Not stated". Do not guess.
- Do not give medical advice or a diagnosis.
- Keep it under 90 words.
- Output only the four lines below, in this order, with no extra commentary.

SUMMARY: <patient name> (<age>) has <conditions, comma separated>.
VITALS: <each reading with its value, and whether it is Normal, Elevated, or High>.
MEDICATIONS: <medication names, comma separated>.
STATUS: <one short sentence on what needs follow-up>.

FORM TEXT:
${"\"\"\""}
$body
${"\"\"\""}
        """.trimIndent()
    }

    fun extraction(scannedText: String): String {
        val body = scannedText.take(OnDeviceAi.MAX_PROMPT_CHARS - 900)
        return """
Read the scanned patient form below and copy out the values you can see.

Rules:
- Copy values EXACTLY as printed. Never invent, convert, or estimate anything.
- Write "-" for any value the form does not show. Never guess.
- Output only the lines listed below, one per line, nothing else.
- For MED and LAB lines use "|" between the parts.

INSURANCE: <insurance company>
MEMBER_ID: <member id>
GROUP: <group number>
PHYSICIAN: <primary care physician>
REASON: <reason for visit, max 10 words>
BP: <blood pressure>
PULSE: <pulse>
TEMP: <temperature>
OXYGEN: <oxygen saturation>
WEIGHT: <weight>
HEIGHT: <height>
BMI: <bmi>
PHARMACY: <pharmacy name>
PHARMACY_PHONE: <pharmacy phone>
ALLERGIES: <allergies>
MED1: <name | dose | frequency>
MED2: <name | dose | frequency>
MED3: <name | dose | frequency>
LAB1: <test | result | unit>
LAB2: <test | result | unit>
LAB3: <test | result | unit>

SCANNED FORM:
${"\"\"\""}
$body
${"\"\"\""}
        """.trimIndent()
    }

    fun formSummary(formText: String): String {
        val body = formText.take(OnDeviceAi.MAX_PROMPT_CHARS - 700)
        return """
You are a clinical intake assistant summarising a completed patient check-in.

Rules:
- Use ONLY facts that appear in the CHECK-IN below. Never invent a value.
- If a section has no information, write "Not stated". Do not guess.
- Do not give medical advice or a diagnosis.
- Keep it under 90 words.
- Output only the four lines below, in this order, with no extra commentary.

SUMMARY: <patient name> (<age>) presenting for <reason>.
VITALS: <each reading with its value, and whether it is Normal, Elevated, or High>.
MEDICATIONS: <medication names, comma separated>.
STATUS: <one short sentence on what needs follow-up>.

CHECK-IN:
${"\"\"\""}
$body
${"\"\"\""}
        """.trimIndent()
    }
}
