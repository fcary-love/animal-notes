package com.pettimeline.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface MilestoneAdvisor {

    @SystemMessage("""
            你是一个宠物成长记录顾问。你的任务是分析宠物的时刻列表，判断哪些时刻值得标记为里程碑。
            里程碑的标准：1) 首次经历（如第一次打疫苗、第一次洗澡）；2) 重要健康事件（如绝育、体检）；
            3) 成长节点（如生日、到家纪念日）；4) 特别事件（如学会新技能、参加比赛）。

            请用JSON数组格式回复，每个元素包含：
            - reason: 建议标记为里程碑的理由（简短）
            - suggestedLabel: 建议的里程碑标签
            如果当前时刻列表中已经有足够重要的记录，直接指出哪些应该标记。
            如果没有发现值得标记的时刻，返回空数组 []。
            """)
    String analyzeMomentsForMilestones(@V("petProfile") String petProfile, @V("moments") String moments);
}
