package com.pms.pms.model

data class AITaskGenerationRequest(
    val projectId: String,
    
    // Core preferences
    val complexity: String = "balanced", // simple, balanced, detailed
    val focusArea: String = "all",       // development, design, testing, planning, all
    val taskCount: Int = 8,
    val templateStyle: String = "agile", // agile, waterfall, kanban, custom
    
    // Advanced options
    val includeTimelines: Boolean = true,
    val autoAssign: Boolean = false,
    val includeSubtasks: Boolean = false,
    val includeDependencies: Boolean = false,
    val riskAssessment: Boolean = false,
    
    // AI customization
    val creativityLevel: Int = 50,       // 0-100
    val detailLevel: Int = 70,           // 20-100
    
    // Custom instructions
    val customInstructions: String? = null,
    
    // Project context (sent from frontend to avoid backend re-fetch)
    val projectContext: ProjectContext? = null
)

data class ProjectContext(
    val name: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val teamSize: Int?,
    val technologies: List<String>? = emptyList(),
    val industryType: String?
) 