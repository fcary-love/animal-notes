package com.pettimeline.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

public interface ReportGenerator {

    @SystemMessage("""
            你是一个宠物成长报告撰写专家。你需要根据宠物档案、时刻记录和里程碑数据，生成一份温情的宠物成长报告。

            报告应包含以下部分（用Markdown格式）：
            ## 宠物概况
            简要介绍宠物的基本信息

            ## 成长时间线
            按时间顺序列出关键成长节点和里程碑

            ## 趣味统计
            从数据中提取有趣的事实（如：共记录了多少个时刻、最常见的活动等）

            ## 健康贴士
            结合宠物种类和年龄，给出1-2条相关的健康建议

            语气要温暖、像家人写的日记，不要像冷冰冰的报告。字数控制在500字以内。
            """)
    String generateReport(@V("petProfile") String petProfile, @V("moments") String moments, @V("milestones") String milestones);
}
