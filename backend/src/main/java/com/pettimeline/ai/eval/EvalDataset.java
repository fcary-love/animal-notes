package com.pettimeline.ai.eval;

import java.util.List;

/**
 * 20 hand-crafted Q&A pairs for retrieval evaluation.
 * Each entry has a question and keywords that SHOULD appear in the retrieved chunks.
 */
public class EvalDataset {

    public record QAPair(String question, List<String> expectedKeywords) {}

    public static final List<QAPair> DATASET = List.of(
        new QAPair("猫咪应该多久喂一次？", List.of("喂", "每天", "幼猫", "成猫")),
        new QAPair("狗狗能吃巧克力吗？", List.of("巧克力", "有毒", "禁食")),
        new QAPair("幼猫疫苗什么时候打？", List.of("疫苗", "幼猫", "6-8周", "首免")),
        new QAPair("猫咪绝育要注意什么？", List.of("绝育", "禁食", "伊丽莎白", "伤口")),
        new QAPair("宠物皮肤病的症状有哪些？", List.of("皮肤病", "脱毛", "红疹", "瘙痒")),
        new QAPair("如何训练狗狗坐下？", List.of("训练", "坐下", "零食", "正向强化")),
        new QAPair("宠物中暑怎么办？", List.of("中暑", "凉水", "阴凉", "急救")),
        new QAPair("新猫咪到家需要准备什么？", List.of("准备", "猫砂", "食盆", "适应")),
        new QAPair("狗狗需要多久驱虫一次？", List.of("驱虫", "每月", "三个月", "体内外")),
        new QAPair("猫咪乱抓家具怎么纠正？", List.of("猫抓板", "行为", "正向强化")),
        new QAPair("成年猫每天吃什么好？", List.of("成年猫", "高蛋白", "猫粮", "罐头")),
        new QAPair("狗狗疫苗和猫咪疫苗有什么区别？", List.of("疫苗", "核心疫苗", "狂犬", "加强")),
        new QAPair("宠物绝育有什么好处？", List.of("绝育", "生殖", "疾病", "行为")),
        new QAPair("猫咪为什么不在猫砂盆上厕所？", List.of("猫砂", "健康", "清洁", "偏好")),
        new QAPair("狗狗肥胖怎么控制？", List.of("肥胖", "热量", "零食", "过量")),
        new QAPair("猫咪体外驱虫用什么药？", List.of("驱虫", "体外", "跳蚤", "蜱虫")),
        new QAPair("宠物受伤出血怎么处理？", List.of("出血", "止血", "按压", "兽医")),
        new QAPair("小狗多大可以开始训练？", List.of("训练", "幼犬", "正向", "零食")),
        new QAPair("猫咪真菌感染会传染给人吗？", List.of("真菌", "传染", "隔离", "环境")),
        new QAPair("老年宠物需要注意什么？", List.of("饮食", "健康", "疾病", "体检"))
    );
}
