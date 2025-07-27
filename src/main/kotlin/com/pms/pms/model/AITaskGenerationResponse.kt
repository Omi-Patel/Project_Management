package com.pms.pms.model

data class AITaskGenerationResponse(
    val success: Boolean,
    val message: String,
    val generatedTasks: List<TaskResponse>,
    val analysis: ProjectAnalysis? = null,
    val metadata: GenerationMetadata? = null
)

data class ProjectAnalysis(
    val projectComplexity: String,
    val estimatedDuration: String,
    val recommendedTeamSize: Int,
    val riskFactors: List<String>,
    val keyMilestones: List<String>,
    val optimizationSuggestions: List<String>
)

data class GenerationMetadata(
    val generationTime: Long,
    val aiModel: String,
    val confidenceScore: Double, // 0.0 - 1.0
    val preferencesUsed: PreferencesSnapshot
)

data class PreferencesSnapshot(
    val complexity: String,
    val focusArea: String,
    val taskCount: Int,
    val creativityLevel: Int,
    val detailLevel: Int
) 