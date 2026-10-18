package com.app.aisheildme.bytrix.tier3_negotiator.agents

import com.app.aisheildme.bytrix.tier2_vault.model.DataCategory
import com.app.aisheildme.bytrix.tier2_vault.model.Domain
import com.app.aisheildme.bytrix.tier2_vault.model.ServiceType
import com.app.aisheildme.bytrix.tier2_vault.repository.AuthorizedDataAccess
import com.app.aisheildme.bytrix.tier2_vault.repository.BankingRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.DefaultPolicies
import com.app.aisheildme.bytrix.tier2_vault.repository.HealthcareRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.RetailRepository
import com.app.aisheildme.bytrix.tier2_vault.repository.SubjectRef
import com.app.aisheildme.bytrix.tier2_vault.repository.VendorRepository
import com.app.aisheildme.bytrix.tier3_negotiator.AgentPlan
import com.app.aisheildme.bytrix.tier3_negotiator.FollowUp
import com.app.aisheildme.bytrix.tier3_negotiator.OutboundPlan
import com.app.aisheildme.bytrix.tier3_negotiator.RecognizedIntent
import com.app.aisheildme.bytrix.tier3_negotiator.ResultItem
import com.app.aisheildme.bytrix.tier3_negotiator.ShieldIntent
import com.app.aisheildme.bytrix.tier3_negotiator.UserRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthcareAgent @Inject constructor(
    private val healthcare: HealthcareRepository,
    private val vendorSelection: VendorSelectionAgent,
) : DomainAgent {
    override val domain = Domain.HEALTHCARE

    override suspend fun plan(ctx: AgentContext): AgentPlan {
        val intent = ctx.recognized.intent
        val patient = healthcare.patient(ctx.userId)
            ?: return AgentPlan(
                intent, "Your patient profile isn't in the vault yet.",
                "Add your patient details or load sample data from the Personal Vault.",
                missingInformation = listOf("Patient profile"), followUp = FollowUp.OPEN_VAULT,
            )

        return when (intent) {
            ShieldIntent.APPOINTMENT_LOOKUP -> {
                val upcoming = healthcare.upcomingAppointments(patient.id, ctx.now)
                AgentPlan(
                    intent,
                    if (upcoming.isEmpty()) "You have no upcoming appointments on file." else "You have ${upcoming.size} upcoming appointment(s).",
                    "Showing appointments from your vault (nothing was shared).",
                    items = upcoming.map {
                        ResultItem(
                            "${it.providerName ?: "Appointment"}${it.specialty?.let { s -> " · $s" } ?: ""}",
                            "${Fmt.dateTime(it.startsAt)} · ${it.organizationName ?: it.location ?: ""}",
                            SubjectRef(AuthorizedDataAccess.SUBJECT_APPOINTMENT, it.id),
                        )
                    },
                )
            }

            ShieldIntent.APPOINTMENT_CHECK_IN -> {
                val next = healthcare.upcomingAppointments(patient.id, ctx.now, limit = 1).firstOrNull()
                    ?: return AgentPlan(intent, "No upcoming appointment to check in for.", "Add the appointment to your vault first.", missingInformation = listOf("Upcoming appointment"), followUp = FollowUp.OPEN_VAULT)
                val vendor = vendorSelection.choose(Domain.HEALTHCARE, ServiceType.APPOINTMENT_CHECK_IN, next.organizationVendorId, next.organizationName)
                val missing = buildList {
                    if (patient.dateOfBirth.isNullOrBlank()) add("Date of birth")
                    if (healthcare.insurance(patient.id).isEmpty()) add("Insurance card")
                }
                AgentPlan(
                    intent,
                    "Check in for ${next.providerName ?: "your appointment"} on ${Fmt.dateTime(next.startsAt)}.",
                    "Send your check-in details to ${vendor?.name ?: next.organizationName ?: "the clinic"}.",
                    items = listOf(ResultItem(next.providerName ?: "Appointment", "${Fmt.dateTime(next.startsAt)} · ${next.organizationName.orEmpty()}", SubjectRef(AuthorizedDataAccess.SUBJECT_APPOINTMENT, next.id))),
                    outbound = OutboundPlan(
                        purpose = intent.purpose!!,
                        vendorId = vendor?.vendorId,
                        vendorName = vendor?.name ?: next.organizationName,
                        subject = SubjectRef(AuthorizedDataAccess.SUBJECT_APPOINTMENT, next.id),
                        requestedCategories = DefaultPolicies.matrix.getValue(intent.purpose),
                        operation = "Appointment check-in",
                    ),
                    missingInformation = missing,
                    followUp = FollowUp.OPEN_CHECK_IN_FORM,
                )
            }

            ShieldIntent.MEDICAL_FORM_COMPLETION -> AgentPlan(
                intent,
                "I can fill a medical form from your vault.",
                "Scan the form; I'll read it on-device, match its fields to your vault and show everything for review before anything is shared.",
                followUp = FollowUp.OPEN_DOCUMENT_IMPORT,
            )

            ShieldIntent.MEDICATION_INFO -> {
                val meds = healthcare.medications(patient.id)
                AgentPlan(
                    intent,
                    if (meds.isEmpty()) "No active medications on file." else "${meds.size} active medication(s).",
                    "Showing medications from your vault.",
                    items = meds.map { ResultItem(it.name, listOfNotNull(it.dosage.ifBlank { null }, it.frequency).joinToString(" · ")) },
                )
            }

            ShieldIntent.INSURANCE_INFO -> {
                val policies = healthcare.insurance(patient.id)
                AgentPlan(
                    intent,
                    if (policies.isEmpty()) "No insurance on file." else "Insurance on file:",
                    if (policies.isEmpty()) "Scan your insurance card to add it." else "Showing insurance from your vault.",
                    items = policies.map { ResultItem(it.payerName, "Member ••${it.memberId.takeLast(4)}${it.groupNumber?.let { g -> " · Group $g" } ?: ""}") },
                    followUp = if (policies.isEmpty()) FollowUp.OPEN_DOCUMENT_IMPORT else null,
                )
            }

            ShieldIntent.PROVIDER_COMMUNICATION -> {
                val next = healthcare.upcomingAppointments(patient.id, ctx.now, limit = 1).firstOrNull()
                val vendor = vendorSelection.choose(Domain.HEALTHCARE, ServiceType.MESSAGING, next?.organizationVendorId, ctx.recognized.slots["counterparty"])
                AgentPlan(
                    intent,
                    "Prepare a message to ${vendor?.name ?: "your provider"}.",
                    "Send a secure message through the provider's authorized channel.",
                    outbound = OutboundPlan(
                        purpose = intent.purpose!!, vendorId = vendor?.vendorId, vendorName = vendor?.name,
                        subject = next?.let { SubjectRef(AuthorizedDataAccess.SUBJECT_APPOINTMENT, it.id) },
                        requestedCategories = setOf(DataCategory.NAME, DataCategory.DATE_OF_BIRTH, DataCategory.APPOINTMENT),
                        operation = "Secure message",
                        notes = mapOf("message" to ctx.request.text.take(300)),
                    ),
                )
            }

            else -> AgentPlan(intent, "I can't handle that healthcare request yet.", "Try: check in, show appointments, medications or insurance.")
        }
    }
}
