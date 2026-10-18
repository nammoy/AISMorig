package com.app.aisheildme.bytrix.feature.checkin

import com.app.aisheildme.bytrix.domain.model.AppointmentType
import com.app.aisheildme.bytrix.domain.model.CheckInForm

object CheckInFormText {

    fun render(form: CheckInForm): String = buildList {
        add("Name: ${form.fullName}")
        add("Age: ${form.age}")
        add("Gender: ${form.gender}")
        add("Appointment: ${form.appointmentType.label}")
        add("Reason for visit: ${form.reasonForVisit}")

        if (form.symptoms.isNotEmpty()) {
            add("Symptoms: ${form.symptoms.joinToString(", ")}")
        }
        addValue("Other symptoms", form.otherSymptom)
        add("Pain level: ${form.painLevel}/10")

        addValue("Known conditions", form.knownConditions)
        addValue("Known allergies", form.knownAllergies)
        addValue("New allergies", form.newAllergyDetail.takeIf { form.hasNewAllergies })

        addValue("Blood pressure", form.bloodPressure)
        addValue("Pulse", form.pulse)
        addValue("Temperature", form.temperature)
        addValue("Oxygen saturation", form.oxygenSaturation)
        addValue("Weight", form.weight)
        addValue("Height", form.height)
        addValue("BMI", form.bmi)

        form.filledMedications.forEach {
            add("Medication: ${it.name} ${it.dose} ${it.frequency}".trim())
        }
        form.filledLabResults.forEach {
            add("Lab: ${it.testName} ${it.resultValue} ${it.unit}".trim())
        }

        addValue("Other details", form.otherDetails)
    }.joinToString("\n")

    private fun MutableList<String>.addValue(label: String, value: String?) {
        val v = value?.trim()
        if (v.isNullOrEmpty() || v == "—") return
        add("$label: $v")
    }
}

fun CheckInFormState.toCheckInForm(): CheckInForm = CheckInForm(
    clinicName = orDash(clinicName),
    checkInAtEpochMs = System.currentTimeMillis(),
    appointmentType = appointmentType,
    otherAppointmentType = otherAppointmentType.trim(),
    patientId = patientId,
    fullName = fullName,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    age = age,
    gender = gender,
    bloodType = bloodType,
    phone = phone,
    email = email,
    address = address,
    street = street,
    city = city,
    state = state,
    zipCode = zipCode,
    emergencyName = emergencyName,
    emergencyRelation = emergencyRelation,
    emergencyPhone = emergencyPhone,
    knownConditions = knownConditions,
    knownAllergies = knownAllergies,
    knownMedications = knownMedications,
    insuranceCompany = orDash(insuranceCompany),
    memberId = orDash(memberId),
    groupNumber = orDash(groupNumber),
    primaryCarePhysician = orDash(physician),
    reasonForVisit = reasonForVisit.trim(),
    symptoms = symptoms.toList(),
    otherSymptom = otherSymptom.trim(),
    painLevel = painLevel,
    medicationsChanged = medicationsChanged,
    medicationChangeDetail = orDash(medicationChangeDetail),
    medications = medications,
    hasNewAllergies = hasNewAllergies,
    newAllergyDetail = orDash(newAllergyDetail),
    weight = orDash(weight),
    height = orDash(height),
    bloodPressure = orDash(bloodPressure),
    pulse = orDash(pulse),
    temperature = orDash(temperature),
    oxygenSaturation = orDash(oxygen),
    bmi = orDash(bmi),
    labResults = labResults,
    pharmacyName = orDash(pharmacyName),
    pharmacyPhone = orDash(pharmacyPhone),
    consentGiven = consentGiven,
    signature = signature,
    otherDetails = otherDetails.trim(),
)

private fun orDash(value: String): String = value.trim().ifEmpty { "—" }

internal val APPOINTMENT_TYPES: List<AppointmentType> = AppointmentType.entries
