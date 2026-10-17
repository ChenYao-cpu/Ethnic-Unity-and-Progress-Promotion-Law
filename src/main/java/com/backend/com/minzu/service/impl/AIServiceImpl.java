package com.backend.com.minzu.service.impl;

import com.backend.com.minzu.common.Result;
import com.backend.com.minzu.entity.AiChat;
import com.backend.com.minzu.entity.LawArticle;
import com.backend.com.minzu.mapper.AiChatMapper;
import com.backend.com.minzu.service.AIService;
import com.backend.com.minzu.service.LawArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements AIService {

    // ==================== DeepSeek API 配置（从 application.yml 注入，有默认兜底值） ====================
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepseekModel;

    @Autowired
    private LawArticleService lawArticleService;

    @Autowired
    private AiChatMapper aiChatMapper;

    // DB 法条缓存（启动时加载，避免每次请求都查库）
    private static volatile Map<String, String> dbArticleCache = null;

    @PostConstruct
    public void initArticleCache() {
        refreshArticleCache();
    }

    private synchronized void refreshArticleCache() {
        if (lawArticleService != null) {
            try {
                List<LawArticle> articles = lawArticleService.getAllArticles();
                if (articles != null && !articles.isEmpty()) {
                    Map<String, String> cache = new LinkedHashMap<>();
                    for (LawArticle a : articles) {
                        if (a.getArticleNumber() != null) {
                            cache.put(a.getArticleNumber(), a.getContent());
                        }
                    }
                    dbArticleCache = cache;
                }
            } catch (Exception e) {
                // DB 不可用时降级到静态 Map
                System.err.println("[AI] 法条缓存加载失败，降级使用静态数据: " + e.getMessage());
            }
        }
    }

    /**
     * 优先从 DB 缓存获取法条内容，缓存为空则降级到静态 Map
     */
    private String getArticleContent(String articleKey) {
        if (dbArticleCache != null && dbArticleCache.containsKey(articleKey)) {
            return dbArticleCache.get(articleKey);
        }
        return LAW_DATABASE.get(articleKey);
    }

    /**
     * 从 DB 或静态 Map 加载全部法条正文（用于构建 System Prompt）
     */
    private String loadAllArticlesForPrompt() {
        Map<String, String> source = (dbArticleCache != null && !dbArticleCache.isEmpty())
            ? dbArticleCache : LAW_DATABASE;
        StringBuilder sb = new StringBuilder();
        List<String> sortedKeys = new ArrayList<>(source.keySet());
        // 尝试按中文条号排序（Preamble 特殊处理）
        sortedKeys.sort((a, b) -> {
            if ("序言".equals(a)) return -1;
            if ("序言".equals(b)) return 1;
            return Integer.compare(articleKeyToSortOrder(a), articleKeyToSortOrder(b));
        });
        for (String key : sortedKeys) {
            sb.append("「").append(key).append("」").append(source.get(key)).append("\n\n");
        }
        return sb.toString();
    }

    private int articleKeyToSortOrder(String key) {
        try {
            String numStr = key.replace("第", "").replace("条", "");
            return chineseToNumber(numStr);
        } catch (Exception e) {
            return 99;
        }
    }

    // 配置超时，防止 DeepSeek API 调用阻塞整个请求
    private final RestTemplate restTemplate;
    {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 连接超时 10 秒
        factory.setReadTimeout(60000);    // 读取超时 60 秒（AI生成回答需要时间）
        restTemplate = new RestTemplate(factory);
    }

    // 《中华人民共和国民族团结进步促进法》法条数据库
    private static final Map<String, String> LAW_DATABASE = new HashMap<>();
    private static final Map<String, String> KEYWORD_DATABASE = new HashMap<>();

    // 立法解读数据库 — 来源：全国人大、中央统战部、国家民委、CCTV专家解读
    private static final Map<String, String> LAW_INTERPRETATION_DATABASE = new HashMap<>();
    // 概念关键词映射
    private static final Map<String, String> CONCEPT_DATABASE = new HashMap<>();

    static {
        // ==================== 第一章 总则 ====================
        LAW_DATABASE.put("第一条", "为了促进民族团结进步，铸牢中华民族共同体意识，推进中华民族共同体建设，推动实现中华民族伟大复兴，根据宪法，制定本法。");
        LAW_DATABASE.put("第二条", "民族团结进步事业坚持中国共产党的全面领导，高举中国特色社会主义伟大旗帜，坚持马克思列宁主义、毛泽东思想、邓小平理论、'三个代表'重要思想、科学发展观，全面贯彻习近平新时代中国特色社会主义思想，巩固各民族团结奋斗的共同思想政治基础，坚定不移走中国特色解决民族问题的正确道路，为强国建设、民族复兴凝聚力量。");
        LAW_DATABASE.put("第三条", "铸牢中华民族共同体意识，应当引导各族人民牢固树立休戚与共、荣辱与共、生死与共、命运与共的共同体理念，增强中华民族凝聚力。");
        LAW_DATABASE.put("第四条", "推进中华民族共同体建设，应当统筹经济、政治、文化、社会和生态文明建设，全面实现各民族共同繁荣发展，确保各族人民共同当家做主人，构筑中华民族共有精神家园，促进各民族全方位互嵌和广泛交往交流交融，共同守护人与自然和谐共生的生态家园，推动中华民族成为认同度更高、凝聚力更强的命运共同体。");
        LAW_DATABASE.put("第五条", "中华人民共和国公民在法律面前一律平等。中华人民共和国各民族一律平等。禁止对任何民族的歧视和压迫。");
        LAW_DATABASE.put("第六条", "中华民族共同体意识是民族团结之本。国家坚持增进共同性、尊重和包容差异性，促进各民族守望相助、和谐共处，维护中华民族大团结。禁止破坏民族团结和制造民族分裂的行为。");
        LAW_DATABASE.put("第七条", "国家推动各民族共同团结奋斗、共同繁荣发展，促进物质文明、政治文明、精神文明、社会文明、生态文明协调发展，全面推进中华民族发展进步。");
        LAW_DATABASE.put("第八条", "国家坚持和完善民族区域自治制度，维护国家统一和民族团结。");
        LAW_DATABASE.put("第九条", "国家坚持依法治理民族事务，依法保障各族群众合法权益，加强宪法法律宣传教育，增强各族群众的国家意识、公民意识、法治意识，维护社会主义法治的统一、尊严和权威，在法治轨道上推进民族事务治理体系和治理能力现代化。");
        LAW_DATABASE.put("第十条", "中华人民共和国公民有维护国家统一和全国各民族团结的义务，应当维护国家主权、安全、发展利益。民族团结进步事业不受外部势力的干涉。坚决反对一切以民族、宗教、人权等借口对中华人民共和国实施污蔑抹黑、遏制打压、渗透破坏等行为。");

        // ==================== 第二章 构筑共有精神家园 ====================
        LAW_DATABASE.put("第十一条", "国家坚持以社会主义核心价值观为引领，深化爱国主义、集体主义、社会主义教育，引导各族群众弘扬以爱国主义为核心的民族精神和以改革创新为核心的时代精神，坚定对伟大祖国、中华民族、中华文化、中国共产党、中国特色社会主义的认同。");
        LAW_DATABASE.put("第十二条", "国家组织开展中国共产党史、新中国史、改革开放史、社会主义发展史、中华民族发展史宣传教育，引导各族群众牢固树立正确的国家观、历史观、民族观、文化观、宗教观。中华人民共和国公民应当增强国家观念，传承和弘扬爱国主义精神，维护国旗、国歌、国徽等国家象征和标志的尊严。");
        LAW_DATABASE.put("第十三条", "国家发展社会主义先进文化，弘扬革命文化，传承中华优秀传统文化，引导各族群众增进中华文化认同，增强中华文化自信。各民族优秀传统文化都是中华文化的组成部分。国家坚持以社会主义先进文化引领各民族优秀传统文化的创造性转化和创新性发展，支持开展中华优秀传统文化的宣传和推广。");
        LAW_DATABASE.put("第十四条", "国家树立和突出各民族共有共享的中华文化符号和中华民族形象，依托中华民族的文化和自然遗产等资源，推动中华文明标识体系的构建。中华人民共和国公民应当维护中华民族的形象，尊重中华民族形成和发展的历史，不得进行侮辱、贬损和亵渎。");
        LAW_DATABASE.put("第十五条", "国家全面推广普及国家通用语言文字。任何组织和个人不得妨碍公民学习和使用国家通用语言文字。学校及其他教育机构以国家通用语言文字为基本的教育教学用语用字。国家尊重和保障少数民族语言文字的学习和使用，推动少数民族语言文字的规范化、标准化和信息化建设，支持少数民族古籍的保护、整理、研究和利用。");
        LAW_DATABASE.put("第十六条", "各级各类学校及其他教育机构应当将铸牢中华民族共同体意识的要求贯穿教育全过程，融入课堂教学、社会实践、主题教育和网络教育相结合的教育体系。各级各类学校及其他教育机构应当按照国家有关规定使用国家统编教材。");
        LAW_DATABASE.put("第十七条", "国家推动中国自主的中华民族共同体史料体系、话语体系、理论体系建设，支持高等学校、科研机构等单位加强对中华民族共同体重大基础性问题的研究，阐释中华民族和中华文明多元一体格局的历史与内涵。");
        LAW_DATABASE.put("第十八条", "国家将铸牢中华民族共同体意识教育纳入国民教育、干部教育、社会教育体系。各级人民政府应当充分利用中华优秀传统文化资源、红色资源和体现中国特色社会主义建设成就的各类资源，因地制宜采取多种形式，广泛开展铸牢中华民族共同体意识教育。");
        LAW_DATABASE.put("第十九条", "报刊、广播、电视等新闻媒体和出版单位、网络服务提供者应当开展有关铸牢中华民族共同体意识、推进中华民族共同体建设的宣传报道、成就展示等工作。国家推进国际传播能力建设，支持开展对外人文交流。");
        LAW_DATABASE.put("第二十条", "各级人民政府应当推动将铸牢中华民族共同体意识的要求融入家庭、家教和家风建设。未成年人的父母或者其他监护人应当依法履行家庭教育责任，教育和引导未成年人热爱中国共产党、热爱祖国、热爱人民、热爱中华民族，树立中华民族一家亲的观念，不得向未成年人灌输不利于民族团结进步的观念。");
        LAW_DATABASE.put("第二十一条", "国家支持香港特别行政区、澳门特别行政区开展中华民族历史、中华文化和国情教育，引导香港特别行政区同胞、澳门特别行政区同胞自觉维护国家主权、安全、发展利益。国家促进两岸经济文化交流合作，深化两岸各领域融合发展，增进台湾同胞对中华民族的归属感、认同感、荣誉感。");

        // ==================== 第三章 促进交往交流交融 ====================
        LAW_DATABASE.put("第二十二条", "县级以上人民政府应当统筹经济社会发展规划和公共资源配置，推进互嵌式社区环境建设，完善各族群众共居共学、共建共享、共事共乐的社会条件。");
        LAW_DATABASE.put("第二十三条", "县级以上地方人民政府应当将铸牢中华民族共同体意识的要求纳入城市规划、建设、治理和服务全过程，因地制宜完善友好型、包容性、融合式的城市民族工作政策措施。地方各级人民政府应当组织和引导各族群众共同参与社区建设、社区治理、社区活动，支持有关单位和组织提供社会服务，促进各族群众和谐融居。");
        LAW_DATABASE.put("第二十四条", "县级以上人民政府应当加强民族地区之间、民族地区与其他地区之间人口流动服务平台的共建和信息的共享，增强协作能力，提高跨区域政务服务的便利化水平。");
        LAW_DATABASE.put("第二十五条", "县级以上人民政府应当依法保障民族地区之间、民族地区与其他地区之间跨区域就业创业公民的合法权益，支持开展法律法规和政策、国家通用语言文字、职业技能等方面的培训，开展职业指导、职业介绍等服务。");
        LAW_DATABASE.put("第二十六条", "国务院教育行政部门和省级人民政府应当支持民族地区和其他地区高等学校双向跨区域招生，加强人才培养的协作。各级各类学校及其他教育机构应当结合学校和学生的特点，促进各族学生共同学习、共同生活、共同成长进步。");
        LAW_DATABASE.put("第二十七条", "各级人民政府应当支持学校、群团组织和其他社会组织，利用中华民族共同体形成和发展的历史文化资源，开展青少年跨区域社会实践、研学游学和参观考察等交流活动，增强民族自豪感和自信心。");
        LAW_DATABASE.put("第二十八条", "各级人民政府应当鼓励和支持志愿者和志愿服务组织，在各类志愿服务活动中促进各族群众的交流合作和友爱互助。");
        LAW_DATABASE.put("第二十九条", "国家增进各民族文化互鉴融通，鼓励各民族互相欣赏优秀传统文化、互相学习语言文字。各级人民政府应当支持文化工作者和有关单位，创作和展示具有中华文化底蕴、体现各民族交往交流交融的文艺作品。");
        LAW_DATABASE.put("第三十条", "国家发挥旅游业在促进各民族交往交流交融方面的积极作用，推进文化和旅游深度融合发展，开发有利于弘扬中华文化的旅游线路和文化创意产品。");
        LAW_DATABASE.put("第三十一条", "国家支持利用互联网、大数据、人工智能等现代技术手段开展交流活动，鼓励和引导网络产品、服务的提供者制作、传播体现中华民族大团结的作品和信息，营造有利于铸牢中华民族共同体意识的和谐网络环境。任何组织和个人不得以文字、图片和音视频等方式，制作和传播含有民族仇恨、民族歧视等破坏民族团结进步内容的信息。");

        // ==================== 第四章 推动共同繁荣发展 ====================
        LAW_DATABASE.put("第三十二条", "国家贯彻新发展理念，支持民族地区全面深化改革开放、全面融入国家发展战略、提升自我发展能力，加快民族地区高质量发展，推进各民族共同富裕，推动各民族共同迈向社会主义现代化。");
        LAW_DATABASE.put("第三十三条", "县级以上人民政府及其有关部门制定和实施经济社会发展领域的规划计划、政策措施，应当有利于铸牢中华民族共同体意识、推进中华民族共同体建设，有利于维护国家统一、反对分裂，有利于改善民生、凝聚人心。");
        LAW_DATABASE.put("第三十四条", "国家促进区域协调发展，完善区域一体化发展机制和差别化区域支持政策，健全对口支援、东西部协作等帮扶协作机制。国家支持民族地区结合区域主体功能定位，统筹发展和安全，在维护国家边疆安全、资源能源安全、粮食安全和生态安全等方面担起使命责任、充分发挥作用。");
        LAW_DATABASE.put("第三十五条", "国务院有关部门和县级以上地方人民政府应当加强交通、能源、水利、信息、物流等基础设施建设，推进民族地区与其他地区之间的互联互通。");
        LAW_DATABASE.put("第三十六条", "国家建设现代化产业体系，因地制宜发展新质生产力，有序推进区域间的产业合作和利益共享机制建设，支持民族地区在优化区域产业链和供应链布局、推进全国统一大市场建设中充分发挥作用。");
        LAW_DATABASE.put("第三十七条", "国务院有关部门和县级以上地方人民政府应当推动就业、教育、医疗等方面公共服务资源的合理配置，增强基本公共服务均衡性和可及性，促进民族地区各级各类教育协调发展，提高民族地区公共服务保障能力。");
        LAW_DATABASE.put("第三十八条", "国务院有关部门和县级以上地方人民政府应当统筹优化农业、生态、城镇等各类空间布局，加强民族地区的生态环境保护和自然资源的可持续利用，引导各族群众共同维护中华民族永续发展的生态空间。");
        LAW_DATABASE.put("第三十九条", "国务院有关部门和边境地区县级以上地方人民政府应当统筹推进兴边富民、稳边固边，鼓励支援边境地区建设，推进基础设施和公共服务设施建设，改善边境村落人居环境和生产生活条件。");
        LAW_DATABASE.put("第四十条", "国家加强时代新风新貌的培育，推进公民道德建设，开展群众性法治宣传教育和科学技术普及，推动移风易俗，倡导文明进步的新风尚，引导各族群众在生活交往、婚丧嫁娶等活动中自觉遵守法律法规、遵循公序良俗。国家保护公民婚姻自由。任何组织和个人不得以民族身份、风俗习惯、宗教信仰等为由干涉婚姻自由。");

        // ==================== 第五章 保障与监督 ====================
        LAW_DATABASE.put("第四十一条", "坚持和完善党委统一领导、政府依法管理、统一战线工作部门牵头协调、民族工作部门履职尽责、各部门通力合作、全社会共同参与的民族工作格局，健全民族工作协调机制。");
        LAW_DATABASE.put("第四十二条", "中央和国家机关各部门、地方各机关各部门在各自职责范围内开展铸牢中华民族共同体意识、促进民族团结进步工作。各机关对在本单位内发生的破坏民族团结进步的行为应当及时予以制止。公职人员在履行职责和日常生活中，应当发挥铸牢中华民族共同体意识、促进民族团结进步的模范带头作用。");
        LAW_DATABASE.put("第四十三条", "各级人民代表大会和县级以上人民代表大会常务委员会依照法定职权，在各项工作中落实铸牢中华民族共同体意识、推进中华民族共同体建设的要求。");
        LAW_DATABASE.put("第四十四条", "工会、共产主义青年团、妇女联合会、工商业联合会、文学艺术界联合会、作家协会、科学技术协会、归国华侨联合会、台湾同胞联谊会、残疾人联合会和其他群团组织，应当发挥各自优势，面向所联系的领域开展铸牢中华民族共同体意识工作，团结所联系的群体为中华民族共同体建设贡献力量。");
        LAW_DATABASE.put("第四十五条", "企业事业组织应当遵守社会公德，履行社会责任，将铸牢中华民族共同体意识的要求融入业务培训、文化建设等活动中，促进民族团结进步。");
        LAW_DATABASE.put("第四十六条", "宗教团体、宗教院校和宗教活动场所，应当开展铸牢中华民族共同体意识宣传教育，坚持我国宗教中国化方向，引导宗教与社会主义社会相适应，引导宗教教职人员、信教群众弘扬爱国主义传统，促进民族和睦、宗教和顺、社会和谐。");
        LAW_DATABASE.put("第四十七条", "基层人民政府应当对居民委员会、村民委员会开展铸牢中华民族共同体意识工作给予指导、支持和帮助。居民委员会、村民委员会应当推动在居民公约、村规民约中体现铸牢中华民族共同体意识的要求，支持和引导各族群众增进团结、互相尊重、互相帮助，促进共同进步。");
        LAW_DATABASE.put("第四十八条", "军队依照本法和有关法律规定，结合国防活动开展铸牢中华民族共同体意识宣传教育，充分利用自身资源保障中华民族共同体建设，巩固和发展军政军民团结，维护国家统一和安全。");
        LAW_DATABASE.put("第四十九条", "国家将铸牢中华民族共同体意识的要求贯穿干部的培养、选拔、使用和管理全过程，加强民族地区干部队伍建设，重视培养和使用少数民族干部。国家支持民族地区专业技术人才和高技能人才的培养，推动民族地区之间、民族地区与其他地区之间的干部和人才交流，引导干部和人才向民族地区基层流动。");
        LAW_DATABASE.put("第五十条", "县级以上人民政府应当将铸牢中华民族共同体意识、促进民族团结进步工作纳入国民经济和社会发展规划及年度计划，将相关工作经费列入预算。县级以上人民政府财政部门依法开展资金使用等情况的指导和监督。");
        LAW_DATABASE.put("第五十一条", "国家将民族事务纳入共建共治共享的社会治理机制，加强组织协调和基层治理力量，强化科技支撑，提升社会治理效能。");
        LAW_DATABASE.put("第五十二条", "国务院有关部门和地方各级人民政府应当贯彻总体国家安全观，健全民族领域重大事项请示报告制度，加强对民族领域重大风险隐患的排查、识别、评估、预警和处置，提升防范化解风险的能力，维护国家安全和社会稳定。");
        LAW_DATABASE.put("第五十三条", "地方各级人民政府和有关机关应当加强矛盾纠纷预防和化解能力建设，发挥多元化纠纷解决机制的作用，准确认定纠纷的性质，依法处理和化解纠纷，维护民族团结。任何组织和个人不得以民族身份、风俗习惯、宗教信仰等为由，故意引发或者激化矛盾，扰乱公共秩序。");
        LAW_DATABASE.put("第五十四条", "公民对破坏民族团结进步的行为有权进行投诉和举报，对国家机关及其工作人员不履行或者不正确履行铸牢中华民族共同体意识、促进民族团结进步法定职责的行为有权进行检举。违反本法规定，破坏民族团结进步，损害国家利益或者社会公共利益的，人民检察院可以依法提起公益诉讼。");
        LAW_DATABASE.put("第五十五条", "开展民族团结进步示范创建工作，应当突出铸牢中华民族共同体意识的要求。对在民族团结进步事业中做出突出贡献的集体和个人，按照国家有关规定给予表彰、奖励。");
        LAW_DATABASE.put("第五十六条", "每年9月的第四周为民族团结进步宣传周，国家通过多种形式集中开展铸牢中华民族共同体意识宣传教育活动。");

        // ==================== 第六章 法律责任 ====================
        LAW_DATABASE.put("第五十七条", "国家机关及其工作人员不履行或者不正确履行本法规定职责的，或者对本法第五十八条、第五十九条、第六十条、第六十一条规定的违法行为未及时予以制止并依法予以处理的，由其主管部门或者有关机关责令改正；造成不良后果或者影响的，对负有责任的领导人员和直接责任人员依法给予处分；构成犯罪的，依法追究刑事责任。");
        LAW_DATABASE.put("第五十八条", "任何组织和个人违反本法有关规定，破坏民族团结进步的，由县级以上人民政府有关部门按照职责及时予以制止、责令改正，并依法给予处罚。构成违反治安管理行为的，由公安机关依法给予治安管理处罚；构成犯罪的，依法追究刑事责任。");
        LAW_DATABASE.put("第五十九条", "任何组织和个人以民族身份为由实施就业歧视、拒绝提供商品或者服务，或者实施法律法规禁止的其他歧视行为的，由县级以上民族工作、人力资源和社会保障、市场监管等有关部门按照职责责令改正；造成不良后果或者影响的，予以警告或者通报批评。法律法规另有处罚规定的，从其规定。");
        LAW_DATABASE.put("第六十条", "社会团体、企业事业组织和其他社会组织对在本单位内发生的破坏民族团结进步的行为，应当及时予以制止；未及时制止，造成不良后果或者影响的，由上级机关或者有关主管部门予以警告或者通报批评，并对直接负责的主管人员和其他直接责任人员依法追究法律责任。");
        LAW_DATABASE.put("第六十一条", "网络运营者违反本法规定，未履行管理责任的，由网信、电信、公安、国家安全、新闻出版、广播电视等有关主管部门按照职责责令改正；拒不改正或者情节严重的，依法给予处罚。");
        LAW_DATABASE.put("第六十二条", "组织、策划、实施暴力恐怖活动、民族分裂活动或者宗教极端活动，构成犯罪的，依法追究刑事责任。煽动或者资助实施前款行为，构成犯罪的，依法追究刑事责任。");
        LAW_DATABASE.put("第六十三条", "中华人民共和国境外的组织和个人，针对中华人民共和国实施破坏民族团结进步、制造民族分裂行为的，依法追究法律责任。");

        // ==================== 第七章 附则 ====================
        LAW_DATABASE.put("第六十四条", "省、自治区、直辖市和设区的市、自治州人民代表大会及其常务委员会，可以结合当地实际情况，制定促进民族团结进步的地方性法规。");
        LAW_DATABASE.put("第六十五条", "本法自2026年7月1日起施行。");

        // ==================== 关键词索引 ====================
        KEYWORD_DATABASE.put("立法目的,中华民族共同体意识,中华民族伟大复兴,宪法,根据宪法,第一条,第1条", "第一条");
        KEYWORD_DATABASE.put("党的领导,中国特色社会主义,民族复兴,解决民族问题道路,强国建设,凝聚力量,第二条,第2条", "第二条");
        KEYWORD_DATABASE.put("共同体意识,四个与共,休戚与共,荣辱与共,生死与共,命运与共,共同体理念,凝聚力,第三条,第3条", "第三条");
        KEYWORD_DATABASE.put("共同体建设,互嵌,交往交流交融,共有精神家园,命运共同体,认同度,第四条,第4条", "第四条");
        KEYWORD_DATABASE.put("平等,民族平等,法律面前一律平等,禁止歧视,禁止压迫,第五条,第5条", "第五条");
        KEYWORD_DATABASE.put("民族团结,增进共同性,尊重差异性,守望相助,禁止破坏民族团结,禁止民族分裂,第六条,第6条", "第六条");
        KEYWORD_DATABASE.put("共同团结奋斗,共同繁荣发展,物质文明,政治文明,精神文明,社会文明,生态文明,第七条,第7条", "第七条");
        KEYWORD_DATABASE.put("民族区域自治,区域自治,维护国家统一,第八条,第8条", "第八条");
        KEYWORD_DATABASE.put("依法治理,法治,国家意识,公民意识,法治意识,宪法法律,治理体系,第九条,第9条", "第九条");
        KEYWORD_DATABASE.put("公民义务,维护国家统一,维护民族团结,维护主权,反干涉,反渗透,第十条,第10条", "第十条");

        KEYWORD_DATABASE.put("社会主义核心价值观,爱国主义,集体主义,民族精神,时代精神,五个认同,第十一条,第11条", "第十一条");
        KEYWORD_DATABASE.put("五史教育,党史,新中国史,改革开放史,社会主义发展史,中华民族发展史,国家观,历史观,民族观,文化观,宗教观,国旗,国歌,国徽,第十二条,第12条", "第十二条");
        KEYWORD_DATABASE.put("中华文化,文化自信,社会主义先进文化,革命文化,优秀传统文化,文化资源,文物保护,非遗,第十三条,第13条", "第十三条");
        KEYWORD_DATABASE.put("中华文化符号,中华民族形象,文明标识,文化符号,形象维护,不得侮辱,第一四条,第14条", "第十四条");
        KEYWORD_DATABASE.put("国家通用语言文字,普通话,少数民族语言文字,双语,学前儿童,统编,古籍,第一五条,第15条", "第十五条");
        KEYWORD_DATABASE.put("教育,统编教材,课堂教学,社会实践,主题教育,网络教育,教材编写,第一六条,第16条", "第十六条");
        KEYWORD_DATABASE.put("史料体系,话语体系,理论体系,学科建设,研究机构,多元一体,交流平台,第一七条,第17条", "第十七条");
        KEYWORD_DATABASE.put("国民教育,干部教育,社会教育,红色资源,共同体意识教育,第一八条,第18条", "第十八条");
        KEYWORD_DATABASE.put("媒体,新闻媒体,宣传报道,国际传播,对外交流,文明互鉴,第一九条,第19条", "第十九条");
        KEYWORD_DATABASE.put("家庭,家教,家风,家庭教育,未成年人,中华民族一家亲,第二十一条,第20条", "第二十条");
        KEYWORD_DATABASE.put("香港,澳门,台湾,两岸,侨胞,海外侨胞,一国两制,第二十一条,第21条", "第二十一条");

        KEYWORD_DATABASE.put("互嵌式社区,共居共学,共建共享,共事共乐,社区环境,第二十二条,第22条", "第二十二条");
        KEYWORD_DATABASE.put("城市民族工作,社区建设,和谐融居,社区治理,包容性,第二十三条,第23条", "第二十三条");
        KEYWORD_DATABASE.put("人口流动,服务平台,跨区域,信息共享,政务服务,第二十四条,第24条", "第二十四条");
        KEYWORD_DATABASE.put("跨区域就业,就业创业,职业培训,职业指导,就业权益,第二十五条,第25条", "第二十五条");
        KEYWORD_DATABASE.put("跨区域招生,人才培养,教师交流,各族学生,共同学习,第二十六条,第26条", "第二十六条");
        KEYWORD_DATABASE.put("青少年,研学,社会实践,交流活动,民族自豪感,第二十七条,第27条", "第二十七条");
        KEYWORD_DATABASE.put("志愿者,志愿服务,交流合作,友爱互助,第二十八条,第28条", "第二十八条");
        KEYWORD_DATABASE.put("文化互鉴,文艺作品,公共文化,传统节日,民俗文化,体育赛事,第二十九条,第29条", "第二十九条");
        KEYWORD_DATABASE.put("旅游业,文旅融合,旅游线路,文化创意,展陈规范,第三十条,第30条", "第三十条");
        KEYWORD_DATABASE.put("互联网,大数据,人工智能,网络环境,民族团结内容,禁止传播,民族仇恨,民族歧视,第三十一条,第31条", "第三十一条");

        KEYWORD_DATABASE.put("新发展理念,高质量发展,共同富裕,现代化,改革开放,第三十二条,第32条", "第三十二条");
        KEYWORD_DATABASE.put("三个有利于,改善民生,凝聚人心,政策制定,第三十三条,第33条", "第三十三条");
        KEYWORD_DATABASE.put("区域协调,对口支援,东西部协作,边疆安全,粮食安全,生态安全,一带一路,第三十四条,第34条", "第三十四条");
        KEYWORD_DATABASE.put("交通,能源,水利,信息,物流,基础设施,互联互通,第三十五条,第35条", "第三十五条");
        KEYWORD_DATABASE.put("现代化产业,新质生产力,特色产业,乡村振兴,城乡融合,传统工艺,传统医药,第三十六条,第36条", "第三十六条");
        KEYWORD_DATABASE.put("公共服务,教育医疗,资源配置,均衡发展,保障能力,第三十七条,第37条", "第三十七条");
        KEYWORD_DATABASE.put("生态保护,生态环境,空间布局,可持续发展,生态空间,第三十八条,第38条", "第三十八条");
        KEYWORD_DATABASE.put("兴边富民,稳边固边,边境,边境贸易,边境旅游,开发开放,第三十九条,第39条", "第三十九条");
        KEYWORD_DATABASE.put("移风易俗,公民道德,法治宣传,公序良俗,婚姻自由,第四十条,第40条", "第四十条");

        KEYWORD_DATABASE.put("民族工作格局,党委领导,政府管理,协调机制,统筹协调,第四十一条,第41条", "第四十一条");
        KEYWORD_DATABASE.put("机关职责,公职人员,模范带头,制止违法行为,第四十二条,第42条", "第四十二条");
        KEYWORD_DATABASE.put("人大,人大常委会,法定职权,第四十三条,第43条", "第四十三条");
        KEYWORD_DATABASE.put("群团组织,工会,共青团,妇联,侨联,台联,残联,第四十四条,第44条", "第四十四条");
        KEYWORD_DATABASE.put("企业,社会组织,行业协会,社会责任,行业自律,职业道德,第四十五条,第45条", "第四十五条");
        KEYWORD_DATABASE.put("宗教,宗教中国化,宗教团体,宗教院校,民族和睦,宗教和顺,第四十六条,第46条", "第四十六条");
        KEYWORD_DATABASE.put("基层,居委会,村委会,村规民约,居民公约,第四十七条,第47条", "第四十七条");
        KEYWORD_DATABASE.put("军队,国防,军政军民团结,维护统一,第四十八条,第48条", "第四十八条");
        KEYWORD_DATABASE.put("干部培养,少数民族干部,人才交流,基层流动,专业技术人才,第四十九条,第49条", "第四十九条");
        KEYWORD_DATABASE.put("经费预算,财政,资金,年度计划,第五十条,第50条", "第五十条");
        KEYWORD_DATABASE.put("社会治理,共建共治共享,科技支撑,第五十一条,第51条", "第五十一条");
        KEYWORD_DATABASE.put("国家安全,总体国家安全观,风险防范,请示报告,风险排查,第五十二条,第52条", "第五十二条");
        KEYWORD_DATABASE.put("矛盾化解,纠纷解决,多元化纠纷解决,维护稳定,第五十三条,第53条", "第五十三条");
        KEYWORD_DATABASE.put("投诉,举报,公益诉讼,检察院,第五十四条,第54条", "第五十四条");
        KEYWORD_DATABASE.put("示范创建,表彰,奖励,突出贡献,第五十五条,第55条", "第五十五条");
        KEYWORD_DATABASE.put("宣传周,9月第四周,集中宣传,第五十六条,第56条", "第五十六条");

        KEYWORD_DATABASE.put("渎职,不作为,处分,刑事责任,责令改正,第五十七条,第57条", "第五十七条");
        KEYWORD_DATABASE.put("破坏民族团结,治安处罚,行政处罚,第五十八条,第58条", "第五十八条");
        KEYWORD_DATABASE.put("就业歧视,拒绝服务,民族歧视,歧视行为,通报批评,第五十九条,第59条", "第五十九条");
        KEYWORD_DATABASE.put("社会组织责任,制止义务,通报批评,责任追究,第六十条,第60条", "第六十条");
        KEYWORD_DATABASE.put("网络运营者,平台责任,管理责任,网信,第六十一条,第61条", "第六十一条");
        KEYWORD_DATABASE.put("暴力恐怖,民族分裂,宗教极端,煽动,资助,第六十二条,第62条", "第六十二条");
        KEYWORD_DATABASE.put("境外,外国,境外组织,境外个人,第六十三条,第63条", "第六十三条");

        KEYWORD_DATABASE.put("地方立法,地方性法规,省级,市级,第六十四条,第64条", "第六十四条");
        KEYWORD_DATABASE.put("施行,生效,施行时间,2026年7月1日,第六十五条,第65条", "第六十五条");
    }

    static {
        // ==================== 法律总览解读 ====================
        LAW_INTERPRETATION_DATABASE.put("OVERVIEW",
            "《中华人民共和国民族团结进步促进法》由十四届全国人大四次会议于2026年3月12日高票通过，" +
            "习近平主席签署第七十一号主席令公布，自2026年7月1日起施行。该法采用「序言+7章」体例，共65条，" +
            "是30多年来第一部设置序言的新制定法律，也是新时代处理民族事务和开展民族工作的基本法律。\n\n" +
            "【三大法律定位】\n" +
            "1. 宣示党和国家关于民族工作大政方针的重要法律\n" +
            "2. 维护中华民族根本利益和整体利益的促进型法律\n" +
            "3. 实施宪法有关规定、处理民族事务和开展民族工作的基本法律\n\n" +
            "【四大立法亮点】主线鲜明、定位精准、体系完整、导向突出\n\n" +
            "【四项立法原则】\n" +
            "1. 坚持正确政治方向——全面贯彻「十二个必须」要求\n" +
            "2. 坚持正确的中华民族历史观——以是否有利于强化中华民族共同性为首要考量\n" +
            "3. 坚持正确把握「四对关系」——共同性和差异性的关系、中华民族共同体意识和各民族意识的关系、" +
            "中华文化和各民族文化的关系、物质和精神的关系\n" +
            "4. 坚持依宪立法——全面贯彻宪法规定、原则和精神\n\n" +
            "【法律主线】全法贯穿「铸牢中华民族共同体意识」这一主线，坚持增进共同性、尊重和包容差异性，" +
            "坚持引导性与约束性并重。");

        // ==================== 核心概念解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CONCEPT_四个与共",
            "「四个与共」即休戚与共、荣辱与共、生死与共、命运与共，是铸牢中华民族共同体意识的核心内涵，" +
            "从利益共同体、价值共同体、安全共同体、命运共同体四个维度定义了中华民族共同体的内在联结。" +
            "这是习近平总书记关于加强和改进民族工作的重要思想的集中体现。");

        LAW_INTERPRETATION_DATABASE.put("CONCEPT_五个认同",
            "「五个认同」即对伟大祖国、中华民族、中华文化、中国共产党、中国特色社会主义的认同，" +
            "是构筑中华民族共有精神家园的核心目标，体现了国家认同、民族认同、文化认同、政治认同和道路认同的统一。");

        LAW_INTERPRETATION_DATABASE.put("CONCEPT_五观",
            "正确「五观」即正确的国家观、历史观、民族观、文化观、宗教观，是开展「五史」宣传教育的目标，" +
            "旨在引导各族群众树立科学的世界观和方法论。");

        LAW_INTERPRETATION_DATABASE.put("CONCEPT_十二个必须",
            "「十二个必须」是习近平总书记关于加强和改进民族工作的重要思想的核心要义，" +
            "全面涵盖了新时代民族工作的方向、原则、方法和路径，本法第一章总则全面贯彻了这一思想。");

        // ==================== 序言解读 ====================
        LAW_INTERPRETATION_DATABASE.put("PREAMBLE",
            "序言阐述了中华民族多元一体的历史格局，回顾了中国共产党在解决民族问题上的历史成就。\n" +
            "【创新亮点】\n" +
            "1. 首次将习近平总书记关于加强和改进民族工作的重要思想写入法律\n" +
            "2. 阐明各民族凝聚成血脉相融、信念相同、文化相通、经济相依、情感相亲的命运共同体（「五个共同」）\n" +
            "3. 明确「民族团结是我国各族人民的生命线」\n" +
            "4. 序言赋予法律统摄性、纲领性的基本法律地位");

        // ==================== 第一章 总则解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_1",
            "第一章「总则」全面贯彻习近平总书记关于加强和改进民族工作的重要思想「十二个必须」的核心要义，" +
            "按四个层次展开：\n" +
            "第一层（第1-2条）：落实宪法规定，明确党的全面领导与指导思想\n" +
            "第二层（第3-4条）：阐释「铸牢中华民族共同体意识」「推进中华民族共同体建设」两大核心概念\n" +
            "第三层（第5-7条）：规定民族平等、民族团结、民族进步等重要原则\n" +
            "第四层（第8-10条）：对民族区域自治制度、依法治理民族事务、维护国家主权安全发展利益作出规定");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_1",
            "本条阐明立法宗旨，属宪法相关法的立法目的条款。四个层次的目标递进：促进民族团结进步→铸牢中华民族共同体意识→" +
            "推进中华民族共同体建设→实现中华民族伟大复兴。「根据宪法」体现了宪法序言及第四条关于「中华人民共和国各民族一律平等」" +
            "「国家维护和发展各民族的平等团结互助和谐关系」等原则为本法的根本法律依据。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_2",
            "本条确立民族团结进步事业的根本政治原则和思想遵循。核心要素：①「党的全面领导」是最高政治原则；" +
            "②完整列举指导思想谱系（马克思列宁主义、毛泽东思想、邓小平理论、「三个代表」重要思想、科学发展观、" +
            "习近平新时代中国特色社会主义思想），体现一脉相承又与时俱进；③「巩固共同思想政治基础」是凝聚各民族团结奋斗的根本；" +
            "④「中国特色解决民族问题的正确道路」是对民族区域自治等制度道路的坚定宣示。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_3",
            "本条对「铸牢中华民族共同体意识」这一全法核心概念作出法定阐释。「四个与共」（休戚与共、荣辱与共、生死与共、" +
            "命运与共）是共同体理念的核心表述，从利益、价值、安全、命运四个维度定义中华民族共同体的内在联结。" +
            "「增强中华民族凝聚力」是直接目标，体现了习近平总书记「必须以铸牢中华民族共同体意识为新时代党的民族工作的主线」的重要论断。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_4",
            "本条阐释「推进中华民族共同体建设」的核心要义。明确五大统筹领域（经济、政治、文化、社会、生态文明），" +
            "确立五个方面工作：全面实现各民族共同繁荣发展、确保各族人民共同当家做主人、构筑中华民族共有精神家园、" +
            "促进各民族全方位互嵌和广泛交往交流交融、共同守护人与自然和谐共生的生态家园。最终目标是「推动中华民族成为认同度更高、" +
            "凝聚力更强的命运共同体」。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_5",
            "本条确立民族平等原则。核心含义：「公民在法律面前一律平等」+「各民族一律平等」，双重保障。" +
            "明确禁止对任何民族的歧视和压迫，体现了宪法第三十三条关于「中华人民共和国公民在法律面前一律平等」和第四条关于" +
            "「中华人民共和国各民族一律平等」「禁止对任何民族的歧视和压迫」规定的具体落实。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_6",
            "本条明确：中华民族共同体意识是「民族团结之本」——这是对民族团结最高程度的定性。" +
            "「坚持增进共同性、尊重和包容差异性」是本法的核心方法论。「禁止破坏民族团结和制造民族分裂的行为」为强制性规范。" +
            "本条是维护民族团结的基石条款。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_8",
            "民族区域自治制度是我国的一项基本政治制度。本条将坚持和完善民族区域自治制度与维护国家统一和民族团结" +
            "紧密关联，强调区域自治不是民族自决或独立，而是在国家统一前提下的自治。" +
            "该制度体现了统一和自治相结合、民族因素和区域因素相结合的原则。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_9",
            "本条确立依法治理民族事务的基本原则。核心要求：①依法保障各族群众合法权益；②加强宪法法律宣传教育；" +
            "③增强国家意识、公民意识、法治意识（「三个意识」）；④维护社会主义法治的统一、尊严和权威；" +
            "⑤在法治轨道上推进民族事务治理体系和治理能力现代化。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_10",
            "本条确立公民义务条款和域外适用原则。两个层次：①我国公民有维护国家统一和全国各民族团结的义务，" +
            "应当维护国家主权、安全、发展利益；②民族团结进步事业不受外部势力干涉，" +
            "坚决反对一切以民族、宗教、人权等借口实施的污蔑抹黑、遏制打压、渗透破坏行为。" +
            "本条的域外效力体现了法律对维护国家统一和民族团结的坚定意志。");

        // ==================== 第二章 构筑共有精神家园解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_2",
            "第二章以「构筑中华民族共有精神家园」为主线，从价值观引领（第11条）→历史认知（第12条）→文化认同（第13-14条）" +
            "→语言文字（第15条）→学校教育（第16条）→理论研究（第17条）→社会教育（第18条）→媒体传播（第19条）" +
            "→家庭教育（第20条）→港澳台与海外（第21条），形成一个由内而外、从观念到制度的完整体系，" +
            "旨在增强全体中华儿女的文化认同和中华民族凝聚力。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_15",
            "本条是语言文字条款。核心精神：推广国家通用语言文字与尊重保障少数民族语言文字二者并行不悖。" +
            "①国家全面推广普及国家通用语言文字，学校以之作为基本教育教学用语用字，" +
            "国家机关以之作为公务用语用字；②同时尊重和保障少数民族语言文字的学习和使用，" +
            "推动其规范化、标准化和信息化建设，支持少数民族古籍的保护、整理、研究和利用。" +
            "这体现了增进共同性和尊重差异性的辩证法。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_20",
            "本条将铸牢中华民族共同体意识融入家庭建设。监护人须履行家庭教育责任，教育引导未成年人" +
            "「热爱中国共产党、热爱祖国、热爱人民、热爱中华民族，树立中华民族一家亲的观念」。" +
            "特别规定「不得向未成年人灌输不利于民族团结进步的观念」，为家庭教育划定了法律底线。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_21",
            "本条涉及国家统一和民族认同的特殊区域。①支持香港、澳门开展国情教育，引导港澳同胞自觉维护国家主权、" +
            "安全、发展利益，在国内法意义上完善了「一国两制」方针的法律体系；" +
            "②促进两岸交流合作，增进台湾同胞对中华民族的归属感、认同感、荣誉感；" +
            "③加强同海外侨胞的联系交流。");

        // ==================== 第三章 促进交往交流交融解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_3",
            "第三章以「促进各民族交往交流交融」为主题，从空间互嵌（第22-23条）→人口流动（第24-25条）" +
            "→教育融合（第26条）→青少年交流（第27条）→社会互助（第28条）→文化互鉴（第29条）" +
            "→旅游促进（第30条）→网络空间（第31条），构建了全方位、多层次的各民族交往交流交融促进体系。" +
            "核心理念是推动各族群众在空间、文化、经济、社会、心理等方面全方位嵌入。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_22",
            "本条提出「互嵌式社区环境」建设，核心内涵是完善各族群众「共居共学、共建共享、共事共乐」（「五共」）" +
            "的社会条件。这是对传统民族聚居模式的超越，旨在通过空间融合促进心理融合。");

        // ==================== 第四章 推动共同繁荣发展解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_4",
            "第四章围绕「推动共同繁荣发展」，贯彻新发展理念。主要内容：\n" +
            "1. 支持民族地区全面融入国家发展战略、加快高质量发展（第32条）\n" +
            "2. 「三个有利于」政策制定原则——有利于铸牢中华民族共同体意识、维护统一反对分裂、改善民生凝聚人心（第33条）\n" +
            "3. 区域协调发展：差别化区域支持政策、对口支援和东西部协作（第34条）\n" +
            "4. 基础设施建设与互联互通（第35条）\n" +
            "5. 因地制宜发展新质生产力、推进产业合作（第36条）\n" +
            "6. 公共服务均等化（第37条）、生态保护与可持续发展（第38条）\n" +
            "7. 兴边富民、稳边固边（第39条）\n" +
            "8. 移风易俗、保护婚姻自由（第40条）");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_33",
            "「三个有利于」是制定实施经济社会发展规划、政策措施的核心标尺：有利于铸牢中华民族共同体意识、" +
            "推进中华民族共同体建设；有利于维护国家统一、反对分裂；有利于改善民生、凝聚人心。" +
            "这要求将所有改革发展赋予彰显中华民族共同体意识的意义。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_40",
            "本条包含两个重要内容：①推进移风易俗——开展公民道德建设、法治宣传教育和科学技术普及，" +
            "引导各族群众在生活交往、婚丧嫁娶等活动中自觉遵守法律法规、遵循公序良俗；" +
            "②婚姻自由保护——国家保护公民婚姻自由，禁止任何组织和个人以民族身份、风俗习惯、宗教信仰等为由干涉婚姻自由。" +
            "该条款在尊重民族风俗习惯和维护公民基本权利之间划定了明确的法律界限。");

        // ==================== 第五章 保障与监督解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_5",
            "第五章构建了「保障与监督」的完整体系：\n" +
            "1. 民族工作格局（第41条）：党委统一领导、政府依法管理、统战部门牵头协调、民族工作部门履职尽责、" +
            "各部门通力合作、全社会共同参与\n" +
            "2. 多主体职责：国家机关和公职人员（第42条）、人大（第43条）、群团组织（第44条）、" +
            "企事业单位和社会组织（第45条）、宗教领域（第46条）、基层自治组织（第47条）、军队（第48条）\n" +
            "3. 保障措施：干部和人才培养（第49条）、财政预算保障（第50条）、社会治理（第51条）、" +
            "国家安全与风险防范（第52条）、矛盾纠纷化解（第53条）\n" +
            "4. 监督机制：公民投诉举报权+检察公益诉讼（第54条）、示范创建与表彰奖励（第55条）、" +
            "民族团结进步宣传周（第56条）");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_54",
            "本条是本法的重要创新条款，建立双重监督机制：\n" +
            "①社会监督——公民有权对破坏民族团结进步的行为进行投诉和举报，对国家机关不履行职责的行为进行检举；\n" +
            "②司法监督——明确赋予检察机关公益诉讼权：违反本法规定，破坏民族团结进步，" +
            "损害国家利益或者社会公共利益的，人民检察院可以依法提起公益诉讼。\n" +
            "将破坏民族团结的行为纳入检察公益诉讼范围，是本法的重大制度创新。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_56",
            "本条设立法定宣传周制度：每年9月的第四周为民族团结进步宣传周，国家通过多种形式集中开展" +
            "铸牢中华民族共同体意识宣传教育活动。这为民族团结宣教提供了制度化的时间载体，" +
            "确保宣传教育持续化、常态化，防止形式主义。");

        // ==================== 第六章 法律责任解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_6",
            "第六章「法律责任」建立了完整的追责体系：\n" +
            "1. 国家机关失职责任（第57条）——不作为或不正确履职、对违法行为未及时制止处理的，责令改正→处分→追刑责\n" +
            "2. 一般破坏民族团结行为（第58条）——行政处罚→治安处罚→刑事追责，三阶递进\n" +
            "3. 民族歧视行为（第59条）——特别列举就业歧视、拒绝提供商品或服务两种常见形态\n" +
            "4. 单位制止义务（第60条）——社会组织须主动制止本单位内的破坏行为，否则实行「双罚制」\n" +
            "5. 网络运营者责任（第61条）——平台须履行管理责任，多部门联合执法\n" +
            "6. 暴力恐怖、民族分裂、宗教极端活动（第62条）——最严厉条款，实施+煽动+资助均入刑\n" +
            "7. 域外适用（第63条）——对境外针对中国的民族分裂行为追责，体现维护国家统一的法律意志");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_62",
            "本条是最为严厉的刑罚条款，针对暴力恐怖活动、民族分裂活动、宗教极端活动三类严重犯罪。" +
            "不仅处罚直接组织实施行为，还将煽动和资助行为纳入刑事追责范围。" +
            "体现了对危害民族团结和国家统一行为的零容忍态度。");

        LAW_INTERPRETATION_DATABASE.put("ARTICLE_63",
            "本条是域外管辖条款（「长臂管辖」），将法律适用范围延伸至中华人民共和国境外。" +
            "境外组织和个人针对我国实施破坏民族团结进步、制造民族分裂行为的，依法追究法律责任。" +
            "这为打击境外反华势力的渗透破坏活动提供了明确的法律武器。");

        // ==================== 第七章 附则解读 ====================
        LAW_INTERPRETATION_DATABASE.put("CHAPTER_7",
            "第七章「附则」共两条：第64条授权省、自治区、直辖市和设区的市、自治州人大及其常委会" +
            "结合当地实际情况制定促进民族团结进步的地方性法规，为各地因地制宜提供了法律空间；" +
            "第65条规定本法自2026年7月1日起施行。");

        // ==================== 概念关键词数据库 ====================
        CONCEPT_DATABASE.put("四个与共", "CONCEPT_四个与共");
        CONCEPT_DATABASE.put("五个认同", "CONCEPT_五个认同");
        CONCEPT_DATABASE.put("五观", "CONCEPT_五观");
        CONCEPT_DATABASE.put("十二个必须", "CONCEPT_十二个必须");
        CONCEPT_DATABASE.put("中华民族共同体意识", "ARTICLE_3§CONCEPT_四个与共");
        CONCEPT_DATABASE.put("共同体意识", "ARTICLE_3§CONCEPT_四个与共");
        CONCEPT_DATABASE.put("中华民族共同体建设", "ARTICLE_4");
        CONCEPT_DATABASE.put("共同体建设", "ARTICLE_4");
        CONCEPT_DATABASE.put("立法目的", "ARTICLE_1");
        CONCEPT_DATABASE.put("指导思想", "ARTICLE_2");
        CONCEPT_DATABASE.put("党的领导", "ARTICLE_2");
        CONCEPT_DATABASE.put("民族区域自治", "ARTICLE_8");
        CONCEPT_DATABASE.put("依法治理", "ARTICLE_9");
        CONCEPT_DATABASE.put("公民义务", "ARTICLE_10");
        CONCEPT_DATABASE.put("外部势力", "ARTICLE_10");
        CONCEPT_DATABASE.put("精神家园", "CHAPTER_2");
        CONCEPT_DATABASE.put("共有精神家园", "CHAPTER_2");
        CONCEPT_DATABASE.put("语言文字", "ARTICLE_15");
        CONCEPT_DATABASE.put("国家通用语言", "ARTICLE_15");
        CONCEPT_DATABASE.put("普通话", "ARTICLE_15");
        CONCEPT_DATABASE.put("家庭教育", "ARTICLE_20");
        CONCEPT_DATABASE.put("家风", "ARTICLE_20");
        CONCEPT_DATABASE.put("香港", "ARTICLE_21");
        CONCEPT_DATABASE.put("澳门", "ARTICLE_21");
        CONCEPT_DATABASE.put("台湾", "ARTICLE_21");
        CONCEPT_DATABASE.put("两岸", "ARTICLE_21");
        CONCEPT_DATABASE.put("交往交流交融", "CHAPTER_3");
        CONCEPT_DATABASE.put("互嵌", "ARTICLE_22§CHAPTER_3");
        CONCEPT_DATABASE.put("共居共学", "ARTICLE_22");
        CONCEPT_DATABASE.put("共同繁荣", "CHAPTER_4");
        CONCEPT_DATABASE.put("高质量发展", "ARTICLE_32");
        CONCEPT_DATABASE.put("共同富裕", "ARTICLE_32");
        CONCEPT_DATABASE.put("三个有利于", "ARTICLE_33");
        CONCEPT_DATABASE.put("对口支援", "ARTICLE_34");
        CONCEPT_DATABASE.put("新质生产力", "ARTICLE_36");
        CONCEPT_DATABASE.put("兴边富民", "ARTICLE_39");
        CONCEPT_DATABASE.put("移风易俗", "ARTICLE_40");
        CONCEPT_DATABASE.put("婚姻自由", "ARTICLE_40");
        CONCEPT_DATABASE.put("民族工作格局", "CHAPTER_5");
        CONCEPT_DATABASE.put("宗教中国化", "ARTICLE_46");
        CONCEPT_DATABASE.put("村规民约", "ARTICLE_47");
        CONCEPT_DATABASE.put("公益诉讼", "ARTICLE_54");
        CONCEPT_DATABASE.put("投诉", "ARTICLE_54");
        CONCEPT_DATABASE.put("举报", "ARTICLE_54");
        CONCEPT_DATABASE.put("宣传周", "ARTICLE_56");
        CONCEPT_DATABASE.put("法律责任", "CHAPTER_6");
        CONCEPT_DATABASE.put("就业歧视", "ARTICLE_59");
        CONCEPT_DATABASE.put("网络运营者", "ARTICLE_61");
        CONCEPT_DATABASE.put("平台责任", "ARTICLE_61");
        CONCEPT_DATABASE.put("暴力恐怖", "ARTICLE_62");
        CONCEPT_DATABASE.put("民族分裂", "ARTICLE_62");
        CONCEPT_DATABASE.put("域外", "ARTICLE_63");
        CONCEPT_DATABASE.put("境外", "ARTICLE_63");
        CONCEPT_DATABASE.put("施行时间", "ARTICLE_65");
        CONCEPT_DATABASE.put("平等", "ARTICLE_5");
        CONCEPT_DATABASE.put("民族团结", "ARTICLE_6");
        CONCEPT_DATABASE.put("生命线", "ARTICLE_6");
    }

    // ==================== AI问答接口 ====================

    @Override
    public Object askQuestion(Long userId, String question) {
        try {
            System.out.println("[AI-RAG+微调] 智能问答: " + (question != null ? question.substring(0, Math.min(30, question.length())) : "null"));
            // RAG检索 + 微调模拟：检索相关法条 + 注入完整知识库
            MatchResult matchResult = findRelatedContent(question);
            String systemPrompt = buildCombinedPrompt(matchResult);
            String aiAnswer = callDeepSeek(systemPrompt, question);

            if (aiAnswer != null && !aiAnswer.startsWith("抱歉") && !aiAnswer.startsWith("调用AI服务失败")) {
                saveChat(userId, "[智能问答]" + question, aiAnswer);
                return Result.success(aiAnswer);
            }
            return Result.success(generateLocalAnswer(question));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(generateLocalAnswer(question));
        }
    }

    /** RAG检索 + 微调知识库 融合提示词 */
    private String buildCombinedPrompt(MatchResult matchResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是《中华人民共和国民族团结进步促进法》的专家助手，同时使用RAG检索增强和领域微调技术。\n\n");
        prompt.append("【回答规则】\n");
        prompt.append("1. 必须引用具体法条编号，格式如「根据第X条」\n");
        prompt.append("2. 区分法条原文和立法解读\n");
        prompt.append("3. 回答结构：核心结论 → 法条依据 → 深度分析 → 实际应用\n\n");
        // RAG检索结果
        prompt.append("【RAG检索到的相关法条】\n");
        if (!matchResult.articleKeys.isEmpty()) {
            for (String key : matchResult.articleKeys) {
                String content = getArticleContent(key);
                if (content != null) prompt.append("「").append(key).append("」").append(content).append("\n");
            }
        }
        if (!matchResult.interpretationKeys.isEmpty()) {
            prompt.append("\n【检索到的相关解读】\n");
            for (String key : matchResult.interpretationKeys) {
                String interp = LAW_INTERPRETATION_DATABASE.get(key);
                if (interp != null) prompt.append(interp).append("\n");
            }
        }
        // 微调知识库（法律总览）
        String overview = LAW_INTERPRETATION_DATABASE.get("OVERVIEW");
        if (overview != null) {
            prompt.append("\n【微调领域知识——法律总览】\n").append(overview).append("\n");
        }
        return prompt.toString();
    }

    /**
     * 使用本地知识库生成回答（不依赖外部AI API）
     */
    private String generateLocalAnswer(String question) {
        if (question == null || question.trim().isEmpty()) {
            return generateDefaultAnswer(question);
        }

        MatchResult matchResult = findRelatedContent(question);

        // 如果匹配到具体法条，返回法条原文+解读
        if (!matchResult.articleKeys.isEmpty()) {
            StringBuilder answer = new StringBuilder();
            answer.append("📋 根据您的提问，以下是相关法条信息：\n\n");

            for (String articleKey : matchResult.articleKeys) {
                String content = getArticleContent(articleKey);
                if (content != null) {
                    answer.append("【").append(articleKey).append("】\n");
                    answer.append(content).append("\n\n");
                }

                // 查找对应解读
                String articleNum = articleKey.replace("第", "").replace("条", "");
                String interpKey = "ARTICLE_" + articleNum;
                String interp = LAW_INTERPRETATION_DATABASE.get(interpKey);
                if (interp != null) {
                    answer.append("📖 【解读】\n").append(interp).append("\n\n");
                }
            }

            // 附加章节解读
            if (matchResult.matchedChapter != null) {
                String chapterInterp = LAW_INTERPRETATION_DATABASE.get(matchResult.matchedChapter);
                if (chapterInterp != null) {
                    answer.append("📚 【章节背景】\n").append(chapterInterp).append("\n\n");
                }
            }

            return answer.toString();
        }

        // 如果匹配到概念/解读
        if (!matchResult.interpretationKeys.isEmpty()) {
            StringBuilder answer = new StringBuilder();
            answer.append("📋 根据您的问题，以下是相关解读：\n\n");

            for (String key : matchResult.interpretationKeys) {
                String interp = LAW_INTERPRETATION_DATABASE.get(key);
                if (interp != null) {
                    String label = key;
                    if (key.startsWith("ARTICLE_")) {
                        label = "关于第" + key.substring("ARTICLE_".length()) + "条";
                    } else if (key.startsWith("CONCEPT_")) {
                        label = "关于「" + key.substring("CONCEPT_".length()).replace("_", "") + "」";
                    }
                    answer.append("【").append(label).append("】\n");
                    answer.append(interp).append("\n\n");
                }
            }

            return answer.toString();
        }

        // 无匹配结果，返回默认回答
        return generateDefaultAnswer(question);
    }

    @Override
    public Object getChatHistory(Long userId) {
        List<Map<String, Object>> history = new ArrayList<>();
        try {
            List<AiChat> chats = aiChatMapper.getRecentHistory(userId, 50);
            if (chats != null) {
                for (AiChat chat : chats) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", chat.getQuestion());
                    item.put("answer", chat.getAnswer());
                    // 根据问题前缀判断模式并去除前缀
                    String q = chat.getQuestion() != null ? chat.getQuestion() : "";
                    if (q.startsWith("[法条解读]")) {
                        item.put("question", q.substring("[法条解读]".length()));
                        item.put("mode", "interpret");
                    } else if (q.startsWith("[智能问答]")) {
                        item.put("question", q.substring("[智能问答]".length()));
                        item.put("mode", "qa");
                    } else if (q.startsWith("【问答】") || q.startsWith("【法条解读】")) {
                        // 兼容旧前缀
                        item.put("question", q.substring(6));
                        item.put("mode", q.startsWith("【问答】") ? "qa" : "interpret");
                    } else {
                        item.put("question", q);
                        item.put("mode", "qa");
                    }
                    history.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("[AI] 聊天记录加载失败: " + e.getMessage());
        }
        return Result.success(history);
    }

    @Override
    public Object interpretLaw(Long userId, String keyword) {
        // 0. 参数校验
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("请输入要查询的关键词或条文编号。");
        }

        // 1. 查找法条原文和解读（多策略模糊匹配）
        String articleContent = null;
        String articleKey = null;
        String matchedFrom = null; // 记录匹配来源用于日志

        // 策略A：精确匹配条号（支持 "第一条"、"第1条"、"第一条 " 等变体）
        String trimmedKeyword = keyword.trim();
        if (getArticleContent(trimmedKeyword) != null) {
            articleKey = trimmedKeyword;
            articleContent = getArticleContent(trimmedKeyword);
            matchedFrom = "精确条号";
        }

        // 策略B：从用户输入中提取条号（如 "帮我解读第一条" → "第一条"）
        if (articleContent == null) {
            Pattern articlePattern = Pattern.compile("第[一二三四五六七八九十百0-9]+条");
            Matcher matcher = articlePattern.matcher(keyword);
            if (matcher.find()) {
                String extractedArticle = matcher.group();
                if (getArticleContent(extractedArticle) != null) {
                    articleKey = extractedArticle;
                    articleContent = getArticleContent(extractedArticle);
                    matchedFrom = "输入中提取条号";
                }
            }
        }

        // 策略C：关键词索引匹配（双向包含）
        if (articleContent == null) {
            String normalizedKeyword = keyword.replaceAll("[\\s\\p{Punct}]", "");
            for (Map.Entry<String, String> entry : KEYWORD_DATABASE.entrySet()) {
                String[] keywords = entry.getKey().split(",");
                for (String kw : keywords) {
                    if (normalizedKeyword.contains(kw.trim()) || kw.trim().contains(normalizedKeyword)) {
                        articleKey = entry.getValue();
                        articleContent = getArticleContent(articleKey);
                        matchedFrom = "关键词匹配: " + kw.trim();
                        break;
                    }
                }
                if (articleContent != null) break;
            }
        }

        // 策略D：概念数据库匹配（提取关联法条）
        if (articleContent == null) {
            String normalizedKeyword = keyword.replaceAll("[\\s\\p{Punct}]", "");
            for (Map.Entry<String, String> entry : CONCEPT_DATABASE.entrySet()) {
                if (entry.getKey().contains(normalizedKeyword) || normalizedKeyword.contains(entry.getKey())) {
                    String[] targets = entry.getValue().split("§");
                    for (String target : targets) {
                        if (target.startsWith("ARTICLE_")) {
                            String num = target.substring("ARTICLE_".length());
                            articleKey = numToArticleKey(num);
                            if (articleKey != null) {
                                articleContent = getArticleContent(articleKey);
                                if (articleContent != null) break;
                            }
                        } else if (target.startsWith("CHAPTER_") || target.startsWith("CONCEPT_")) {
                            // 记录章节/概念引用，继续查找法条
                            String interp = LAW_INTERPRETATION_DATABASE.get(target);
                            if (interp != null && articleContent == null) {
                                // 如果概念本身无法映射到具体法条，至少我们有章节解读
                                matchedFrom = "概念/章节: " + target;
                            }
                        }
                    }
                    if (articleContent != null) break;
                }
            }
        }

        // 2. 查找立法解读（章节 + 具体法条）
        StringBuilder interpretationContext = new StringBuilder();
        if (articleKey != null) {
            String articleNum = articleKey.replace("第", "").replace("条", "");
            String interpKey = "ARTICLE_" + articleNum;
            String interp = LAW_INTERPRETATION_DATABASE.get(interpKey);
            if (interp != null) {
                interpretationContext.append(interp);
            }
        }

        // 3. 优先使用 DeepSeek AI 生成深度解读，失败降级到预制解读数据库
        Map<String, Object> result = new HashMap<>();
        String aiInterpretation = null;

        if (articleContent != null) {
            // 匹配到具体法条：先尝试 AI 解读
            result.put("articleNumber", articleKey);
            result.put("content", articleContent);

            try {
                String interpretPrompt = buildInterpretPrompt(articleKey, articleContent, interpretationContext.toString());
                System.out.println("[AI] 法条解读 正在调用 DeepSeek... keyword=" + keyword);
                aiInterpretation = callDeepSeek(interpretPrompt, keyword);
                System.out.println("[AI] 法条解读 DeepSeek 返回长度=" + (aiInterpretation != null ? aiInterpretation.length() : 0));
            } catch (Exception e) {
                System.err.println("[AI] 法条解读 DeepSeek 调用异常: " + e.getMessage());
            }

            if (aiInterpretation != null && !aiInterpretation.startsWith("抱歉") && !aiInterpretation.startsWith("调用AI服务失败")) {
                System.out.println("[AI] 法条解读 使用 AI 回答");
                result.put("interpretation", aiInterpretation);
                result.put("source", "ai");
            } else if (interpretationContext.length() > 0) {
                System.err.println("[AI] 法条解读 AI失败，降级本地解读");
                result.put("interpretation", "📌 " + (articleKey != null ? articleKey + " 解读" : "解读") + "\n\n" + interpretationContext.toString());
                result.put("source", "local");
            } else {
                System.err.println("[AI] 法条解读 AI失败，无本地解读");
                String chapterInterp = findChapterInterpretation(articleKey);
                result.put("interpretation", chapterInterp != null ? chapterInterp : "该法条暂无详细解读，以下为法条原文。如需更多信息，请尝试查询相关核心概念（如「民族团结」「平等」「教育」等）。");
                result.put("source", "local");
            }
        } else {
            // 未匹配到具体法条，尝试返回相关综合解读
            String generalInterp = findGeneralInterpretation(keyword);
            if (generalInterp != null) {
                result.put("articleNumber", "综合解读");
                result.put("content", "根据《中华人民共和国民族团结进步促进法》相关条款");
                result.put("interpretation", generalInterp);
            } else {
                result.put("articleNumber", "未精确匹配");
                result.put("content", "未找到与「" + keyword + "」直接对应的法条。");
                result.put("interpretation", "建议您：\n"
                    + "1. 尝试输入具体条文编号（如「第一条」「第十条」）\n"
                    + "2. 使用核心概念查询（如「民族团结」「平等」「语言文字」「教育」等）\n"
                    + "3. 切换到「智能问答」模式进行更灵活的提问");
            }
        }
        // 4. 保存到 ai_chat（法条解读模式）
        try {
            AiChat chat = new AiChat();
            chat.setUserId(userId != null ? userId : 1L);
            chat.setQuestion("[法条解读]" + keyword);
            String answerText = result.containsKey("interpretation")
                ? (String) result.get("interpretation")
                : (String) result.get("content");
            chat.setAnswer(answerText);
            chat.setCreateTime(new Date());
            aiChatMapper.insert(chat);
        } catch (Exception e) {
            System.err.println("[AI] 法条解读 聊天记录保存失败: " + e.getMessage());
        }

        return Result.success(result);
    }

    // ==================== 核心：AI生成回答 ====================

    // 匹配结果内部类
    private static class MatchResult {
        List<String> articleKeys = new ArrayList<>();
        List<String> interpretationKeys = new ArrayList<>();
        String matchedChapter = null;
    }

    /** 向量化语义检索：TF-IDF 风格，将问题与全部法条做余弦相似度计算 */
    private List<String> semanticVectorSearch(String question) {
        List<String> result = new ArrayList<>();
        // 构建问题的词频向量
        Map<String, Integer> queryVec = buildTermVector(question);
        if (queryVec.isEmpty()) return result;
        // 与每条法条的关键词向量计算相似度
        List<Map.Entry<String, Double>> scores = new ArrayList<>();
        for (String articleKey : LAW_DATABASE.keySet()) {
            String articleText = getArticleContent(articleKey);
            if (articleText == null) continue;
            // 法条向量 = 法条正文词频 + 关键词数据库词频
            Map<String, Integer> articleVec = buildTermVector(articleText);
            // 加入该法条在关键词数据库中的扩展词
            for (Map.Entry<String, String> kwEntry : KEYWORD_DATABASE.entrySet()) {
                if (articleKey.equals(kwEntry.getValue())) {
                    for (String kw : kwEntry.getKey().split(",")) {
                        articleVec.merge(kw.trim(), 3, Integer::sum); // 关键词权重x3
                    }
                }
            }
            // 加入立法解读词频（如有）
            String articleNum = articleKey.replace("第", "").replace("条", "");
            String interpKey = "ARTICLE_" + articleNum;
            String interp = LAW_INTERPRETATION_DATABASE.get(interpKey);
            if (interp != null) {
                Map<String, Integer> interpVec = buildTermVector(interp);
                interpVec.forEach((k, v) -> articleVec.merge(k, v, Integer::sum));
            }
            double similarity = cosineSimilarity(queryVec, articleVec);
            if (similarity > 0.05) {
                scores.add(new AbstractMap.SimpleEntry<>(articleKey, similarity));
            }
        }
        // 按相似度降序排列，取Top5
        scores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(5, scores.size()); i++) {
            result.add(scores.get(i).getKey());
        }
        return result;
    }

    /** 将文本转为词频向量（中文分词简化：按字符bigram切分） */
    private Map<String, Integer> buildTermVector(String text) {
        Map<String, Integer> vec = new HashMap<>();
        if (text == null || text.isEmpty()) return vec;
        // 去掉标点和空白
        String cleaned = text.replaceAll("[\\s\\p{Punct}。，、；：「」『』【】《》？！\"'（）\\d]", "");
        // Bigram切分（两个连续字符为一个term）
        for (int i = 0; i < cleaned.length() - 1; i++) {
            String bigram = cleaned.substring(i, Math.min(i + 2, cleaned.length()));
            vec.merge(bigram, 1, Integer::sum);
        }
        // 同时加入单字（提升短词匹配）
        for (int i = 0; i < cleaned.length(); i++) {
            String unigram = cleaned.substring(i, i + 1);
            vec.merge(unigram, 1, Integer::sum);
        }
        return vec;
    }

    /** 余弦相似度 */
    private double cosineSimilarity(Map<String, Integer> v1, Map<String, Integer> v2) {
        if (v1.isEmpty() || v2.isEmpty()) return 0;
        Set<String> allKeys = new HashSet<>(v1.keySet());
        allKeys.addAll(v2.keySet());
        double dotProduct = 0, norm1 = 0, norm2 = 0;
        for (String key : allKeys) {
            double a = v1.getOrDefault(key, 0);
            double b = v2.getOrDefault(key, 0);
            dotProduct += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }
        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String generateAnswer(String question) {
        try {
            MatchResult matchResult = findRelatedContent(question);
            String systemPrompt = buildEnhancedSystemPrompt(matchResult, question);
            String aiAnswer = callDeepSeek(systemPrompt, question);

            if (aiAnswer != null && !aiAnswer.startsWith("抱歉") && !aiAnswer.startsWith("调用AI服务失败")) {
                return aiAnswer;
            }
            return generateFallbackAnswer(question);
        } catch (Exception e) {
            e.printStackTrace();
            return generateFallbackAnswer(question);
        }
    }

    /**
     * 多维度检索：条号 + 关键词 + 概念 + 章节
     */
    private MatchResult findRelatedContent(String question) {
        MatchResult result = new MatchResult();
        String normalizedQuestion = question.replaceAll("[\\s\\p{Punct}]", "");

        // 1. 精确匹配条号
        Pattern articlePattern = Pattern.compile("第([一二三四五六七八九十百]+)条");
        Matcher matcher = articlePattern.matcher(question);
        while (matcher.find()) {
            String articleKey = matcher.group();
            if (getArticleContent(articleKey) != null) {
                result.articleKeys.add(articleKey);
                String articleNum = articleKey.replace("第", "").replace("条", "");
                String interpretationKey = "ARTICLE_" + articleNum;
                if (LAW_INTERPRETATION_DATABASE.containsKey(interpretationKey)) {
                    result.interpretationKeys.add(interpretationKey);
                }
            }
        }

        // 2. 向量化语义检索：将问题与所有法条做TF-IDF相似度计算，找到语义相关的法条
        List<String> semanticArticles = semanticVectorSearch(question);
        for (String key : semanticArticles) {
            if (!result.articleKeys.contains(key)) {
                result.articleKeys.add(key);
            }
        }

        // 3. 原有关键词匹配
        List<String> sortedKeywords = new ArrayList<>(KEYWORD_DATABASE.keySet());
        sortedKeywords.sort((a, b) -> b.length() - a.length());
        for (String keyword : sortedKeywords) {
            if (normalizedQuestion.contains(keyword)) {
                String articleKey = KEYWORD_DATABASE.get(keyword);
                if (!result.articleKeys.contains(articleKey)) {
                    result.articleKeys.add(articleKey);
                }
            }
        }

        // 3. 概念/解读关键词匹配
        for (Map.Entry<String, String> entry : CONCEPT_DATABASE.entrySet()) {
            if (normalizedQuestion.contains(entry.getKey())) {
                String[] targets = entry.getValue().split("§");
                for (String target : targets) {
                    if (!result.interpretationKeys.contains(target)) {
                        result.interpretationKeys.add(target);
                    }
                    if (target.startsWith("ARTICLE_")) {
                        String articleNum = target.substring("ARTICLE_".length());
                        String articleKey = numToArticleKey(articleNum);
                        if (articleKey != null && !result.articleKeys.contains(articleKey)) {
                            result.articleKeys.add(articleKey);
                        }
                    }
                }
            }
        }

        // 4. 章节关键词匹配
        if (normalizedQuestion.contains("精神家园") || normalizedQuestion.contains("共有精神家园")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_2";
        }
        if (normalizedQuestion.contains("交往") || normalizedQuestion.contains("交流") || normalizedQuestion.contains("交融")
            || normalizedQuestion.contains("互嵌") || normalizedQuestion.contains("社区")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_3";
        }
        if (normalizedQuestion.contains("共同繁荣") || normalizedQuestion.contains("高质量发展")
            || normalizedQuestion.contains("共同富裕") || normalizedQuestion.contains("兴边富民")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_4";
        }
        if (normalizedQuestion.contains("保障") || normalizedQuestion.contains("监督") || normalizedQuestion.contains("民族工作格局")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_5";
        }
        if (normalizedQuestion.contains("法律责任") || normalizedQuestion.contains("处罚") || normalizedQuestion.contains("追责")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_6";
        }
        if (normalizedQuestion.contains("总则") || normalizedQuestion.contains("立法目的") || normalizedQuestion.contains("指导思想")) {
            if (result.matchedChapter == null) result.matchedChapter = "CHAPTER_1";
        }

        // 去重，最多5条
        List<String> uniqueArticles = new ArrayList<>();
        for (String a : result.articleKeys) {
            if (!uniqueArticles.contains(a) && uniqueArticles.size() < 5) {
                uniqueArticles.add(a);
            }
        }
        result.articleKeys = uniqueArticles;

        List<String> uniqueInterp = new ArrayList<>();
        for (String i : result.interpretationKeys) {
            if (!uniqueInterp.contains(i) && uniqueInterp.size() < 5) {
                uniqueInterp.add(i);
            }
        }
        result.interpretationKeys = uniqueInterp;

        return result;
    }

    /**
     * 将数字转换为中文条号
     */
    private String numToArticleKey(String num) {
        try {
            int n = Integer.parseInt(num);
            if (n < 1 || n > 65) return null;
            String[] digits = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
            if (n <= 10) return "第" + digits[n] + "条";
            if (n < 20) return "第十" + (n > 10 ? digits[n - 10] : "") + "条";
            if (n < 30) return "第二十" + (n > 20 ? digits[n - 20] : "") + "条";
            if (n < 40) return "第三十" + (n > 30 ? digits[n - 30] : "") + "条";
            if (n < 50) return "第四十" + (n > 40 ? digits[n - 40] : "") + "条";
            if (n < 60) return "第五十" + (n > 50 ? digits[n - 50] : "") + "条";
            if (n <= 65) return "第六十" + (n > 60 ? digits[n - 60] : "") + "条";
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /**
     * 构建增强版系统提示词 —— 三层信息注入 + Few-shot示例
     */
    private String buildEnhancedSystemPrompt(MatchResult matchResult, String question) {
        StringBuilder prompt = new StringBuilder();

        // ===== 第一层：角色定位 + 法律总览 =====
        prompt.append("你是《中华人民共和国民族团结进步促进法》的权威专家助手，" +
            "只基于下面提供的法条原文和官方立法解读回答问题。\n\n");

        String overview = LAW_INTERPRETATION_DATABASE.get("OVERVIEW");
        if (overview != null) {
            prompt.append("【法律总览】\n").append(overview).append("\n\n");
        }

        // ===== 第二层：回答规则 =====
        prompt.append("【回答规则 —— 必须严格遵守】\n");
        prompt.append("1. 每个回答必须引用具体法条编号，格式如「根据第X条」\n");
        prompt.append("2. 区分法条原文和立法解读：引用原文时说「第X条规定：...」；" +
            "解释背景或含义时说「根据立法解读：...」\n");
        prompt.append("3. 只能基于下面提供的法条和解读回答，不要编造或使用训练数据中的猜测\n");
        prompt.append("4. 如果提供的材料不足以完全回答问题，请诚实说明，不要强行编造\n");
        prompt.append("5. 如果问题与《中华人民共和国民族团结进步促进法》完全无关，" +
            "回答：「您的问题超出了本法的范围，建议咨询相关法律专业人士。」\n");
        prompt.append("6. 回答要层次分明、通俗易懂，先给出核心结论，再展开说明\n");
        prompt.append("7. 回答中引用法律概念时，优先使用法律原文的表述\n\n");

        // ===== 第三层A：相关章节解读 =====
        if (matchResult.matchedChapter != null) {
            String chapterInterp = LAW_INTERPRETATION_DATABASE.get(matchResult.matchedChapter);
            if (chapterInterp != null) {
                prompt.append("【相关章节解读】\n").append(chapterInterp).append("\n\n");
            }
        }

        // ===== 第三层B：相关法条原文 =====
        if (!matchResult.articleKeys.isEmpty()) {
            prompt.append("【相关法条原文】\n");
            for (String key : matchResult.articleKeys) {
                String content = getArticleContent(key);
                if (content != null) {
                    prompt.append("「").append(key).append("」").append(content).append("\n");
                }
            }
            prompt.append("\n");
        } else {
            prompt.append("【相关法条原文】\n（未匹配到具体法条，请根据法律总览知识谨慎回答，" +
                "建议提示用户提供更具体的条文编号或关键词）\n\n");
        }

        // ===== 第三层C：相关立法解读 =====
        if (!matchResult.interpretationKeys.isEmpty()) {
            prompt.append("【相关立法解读】\n");
            for (String key : matchResult.interpretationKeys) {
                String interp = LAW_INTERPRETATION_DATABASE.get(key);
                if (interp != null) {
                    String label = key;
                    if (key.startsWith("ARTICLE_")) {
                        label = "关于第" + key.substring("ARTICLE_".length()) + "条";
                    } else if (key.startsWith("CONCEPT_")) {
                        label = "关于" + key.substring("CONCEPT_".length()).replace("_", "") + "";
                    } else if (key.startsWith("CHAPTER_")) {
                        label = "关于第" + key.substring("CHAPTER_".length()) + "章";
                    }
                    prompt.append("【").append(label).append("】").append(interp).append("\n");
                }
            }
            prompt.append("\n");
        }

        // ===== 第四层：Few-shot 示例 =====
        prompt.append("【回答示例】\n");
        prompt.append("示例问题：「什么是中华民族共同体意识？」\n");
        prompt.append("示例回答：\n");
        prompt.append("中华民族共同体意识是《民族团结进步促进法》的核心概念，被明确为「民族团结之本」（第六条）。\n\n");
        prompt.append("根据第三条规定，铸牢中华民族共同体意识应当引导各族人民牢固树立「休戚与共、荣辱与共、" +
            "生死与共、命运与共」的共同体理念。根据立法解读，这四个与共从利益共同体、价值共同体、" +
            "安全共同体、命运共同体四个维度定义了中华民族共同体的内在联结。\n\n");
        prompt.append("这体现了习近平总书记「必须以铸牢中华民族共同体意识为新时代党的民族工作的主线」的重要论断，" +
            "是贯穿全法的立法宗旨和根本遵循。\n");

        return prompt.toString();
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String systemPrompt, String userQuestion) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepseekModel);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 3072);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userQuestion);
            messages.add(userMessage);

            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepseekApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(deepseekUrl, entity, Map.class);

            Map responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map> choices = (List<Map>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map message = (Map) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "抱歉，AI服务暂时不可用，请稍后再试。";

        } catch (Exception e) {
            e.printStackTrace();
            return "调用AI服务失败：" + e.getMessage();
        }
    }

    /**
     * 构建法条解读专用提示词
     */
    private String buildInterpretPrompt(String articleKey, String articleContent, String interpContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是《中华人民共和国民族团结进步促进法》的权威解读专家，" +
            "融合RAG检索增强和领域微调技术。请对以下法条进行专业解读。\n\n");
        // 微调知识库
        String overview = LAW_INTERPRETATION_DATABASE.get("OVERVIEW");
        if (overview != null) {
            prompt.append("【微调领域知识——法律总览】\n").append(overview).append("\n\n");
        }
        // 查找相关章节解读
        String chapterInterp = findChapterInterpretation(articleKey);
        if (chapterInterp != null) {
            prompt.append("【RAG检索——章节背景】\n").append(chapterInterp).append("\n\n");
        }
        // RAG检索到的法条
        prompt.append("【RAG检索——待解读法条】\n");
        prompt.append("「").append(articleKey != null ? articleKey : "").append("」\n");
        prompt.append(articleContent).append("\n\n");
        // 相关立法解读
        if (interpContext != null && interpContext.length() > 0) {
            prompt.append("【RAG检索——立法解读】\n").append(interpContext).append("\n\n");
        }
        prompt.append("【解读规则】\n");
        prompt.append("1. 先用一句话概括核心要义\n");
        prompt.append("2. 逐层解析条文含义和立法意图\n");
        prompt.append("3. 结合立法背景说明该条的意义\n");
        prompt.append("4. 使用通俗语言，让普通群众也能理解\n");
        prompt.append("5. 最后补充一个实际生活中的例子\n\n");
        prompt.append("【回答格式】\n");
        prompt.append("核心要义：\n条文解读：\n立法背景与意义：\n实际应用：");
        return prompt.toString();
    }

    /**
     * 降级方案：AI调用失败时返回法条原文
     */
    private String generateFallbackAnswer(String question) {
        StringBuilder answer = new StringBuilder();
        MatchResult matchResult = findRelatedContent(question);

        if (!matchResult.articleKeys.isEmpty()) {
            answer.append("【AI服务暂时不可用，以下是相关法条原文】\n\n");
            for (String articleKey : matchResult.articleKeys) {
                answer.append("【").append(articleKey).append("】\n");
                answer.append(getArticleContent(articleKey)).append("\n\n");
            }
            // 也输出相关解读
            if (!matchResult.interpretationKeys.isEmpty()) {
                answer.append("【立法解读参考】\n");
                for (String key : matchResult.interpretationKeys) {
                    String interp = LAW_INTERPRETATION_DATABASE.get(key);
                    if (interp != null) {
                        answer.append(interp).append("\n");
                    }
                }
            }
            return answer.toString();
        }

        return generateDefaultAnswer(question);
    }

    private String generateDefaultAnswer(String question) {
        StringBuilder answer = new StringBuilder();
        answer.append("感谢您的提问！\n\n");
        answer.append("《中华人民共和国民族团结进步促进法》于2026年7月1日起施行。\n\n");
        answer.append("我可以为您解答以下方面的问题：\n");
        answer.append("• 立法目的与基本原则\n");
        answer.append("• 构筑共有精神家园\n");
        answer.append("• 促进交往交流交融\n");
        answer.append("• 推动共同繁荣发展\n\n");
        answer.append("您可以：\n");
        answer.append("1. 直接提问，如：「什么是中华民族共同体意识？」\n");
        answer.append("2. 查询具体法条，如：「第五条规定了什么？」\n");
        answer.append("3. 使用关键词搜索，如：「平等」「团结」「教育」等");
        return answer.toString();
    }

    /**
     * 根据条号查找所在章节的解读
     */
    private String findChapterInterpretation(String articleKey) {
        if (articleKey == null) return null;
        try {
            // 从条号中提取数字
            String numStr = articleKey.replace("第", "").replace("条", "");
            int articleNum = chineseToNumber(numStr);
            if (articleNum == -1) return null;

            if (articleNum >= 1 && articleNum <= 10) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_1");
            } else if (articleNum >= 11 && articleNum <= 21) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_2");
            } else if (articleNum >= 22 && articleNum <= 31) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_3");
            } else if (articleNum >= 32 && articleNum <= 40) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_4");
            } else if (articleNum >= 41 && articleNum <= 56) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_5");
            } else if (articleNum >= 57 && articleNum <= 63) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_6");
            } else if (articleNum >= 64 && articleNum <= 65) {
                return LAW_INTERPRETATION_DATABASE.get("CHAPTER_7");
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 根据关键词查找综合解读（概念匹配）
     */
    private String findGeneralInterpretation(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return null;

        // 先尝试概念数据库匹配
        String normalizedKeyword = keyword.replaceAll("[\\s\\p{Punct}]", "");
        for (Map.Entry<String, String> entry : CONCEPT_DATABASE.entrySet()) {
            if (entry.getKey().contains(normalizedKeyword) || normalizedKeyword.contains(entry.getKey())) {
                String[] targets = entry.getValue().split("§");
                for (String target : targets) {
                    String interp = LAW_INTERPRETATION_DATABASE.get(target);
                    if (interp != null) {
                        return "【相关概念：" + entry.getKey() + "】\n\n" + interp;
                    }
                }
            }
        }

        // 再尝试关键词索引匹配
        for (Map.Entry<String, String> entry : KEYWORD_DATABASE.entrySet()) {
            String[] keywords = entry.getKey().split(",");
            for (String kw : keywords) {
                if (normalizedKeyword.contains(kw.trim()) || kw.trim().contains(normalizedKeyword)) {
                    String articleKey = entry.getValue();
                    String content = getArticleContent(articleKey);
                    if (content != null) {
                        String chapterInterp = findChapterInterpretation(articleKey);
                        StringBuilder sb = new StringBuilder();
                        sb.append("【相关法条：「").append(articleKey).append("」】\n\n");
                        sb.append("📜 法条原文：").append(content).append("\n");
                        if (chapterInterp != null) {
                            sb.append("\n📖 章节背景：\n").append(chapterInterp);
                        }
                        return sb.toString();
                    }
                }
            }
        }

        // 法律总览兜底
        return LAW_INTERPRETATION_DATABASE.get("OVERVIEW");
    }

    /**
     * 中文数字转阿拉伯数字（用于条号解析）
     */
    private int chineseToNumber(String chinese) {
        try {
            return Integer.parseInt(chinese);
        } catch (NumberFormatException e) {
            // 尝试中文数字
            Map<Character, Integer> map = new HashMap<>();
            map.put('一', 1); map.put('二', 2); map.put('三', 3); map.put('四', 4);
            map.put('五', 5); map.put('六', 6); map.put('七', 7); map.put('八', 8);
            map.put('九', 9); map.put('十', 0);

            if (chinese.length() == 1) {
                return map.getOrDefault(chinese.charAt(0), -1);
            }
            // 简单处理：二十五 → 25, 六十五 → 65
            if (chinese.length() == 2 && chinese.charAt(1) == '十') {
                return map.getOrDefault(chinese.charAt(0), 0) * 10;
            }
            if (chinese.length() == 2 && chinese.charAt(0) == '十') {
                return 10 + map.getOrDefault(chinese.charAt(1), 0);
            }
            if (chinese.length() == 3 && chinese.charAt(1) == '十') {
                int tens = map.getOrDefault(chinese.charAt(0), 0) * 10;
                int ones = map.getOrDefault(chinese.charAt(2), 0);
                return tens + ones;
            }
            return -1;
        }
    }

    // ==================== AI生成题目 ====================

    @Override
    public List<Map<String, Object>> generateQuestions(Long userId, Integer questionCount,
                                                       Integer questionType, Integer difficulty) {
        List<Map<String, Object>> questions = new ArrayList<>();
        List<Map<String, Object>> questionBank = getQuestionBank();

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> q : questionBank) {
            Integer type = (Integer) q.get("type");
            if (type == null) type = 1;
            if (type == questionType) filtered.add(q);
        }

        java.util.Collections.shuffle(filtered);

        for (int i = 0; i < questionCount && i < filtered.size(); i++) {
            Map<String, Object> question = new HashMap<>(filtered.get(i));
            question.put("questionType", questionType);
            question.put("type", questionType);
            questions.add(question);
        }

        while (questions.size() < questionCount) {
            questions.add(generateGenericQuestion(questionType));
        }

        return questions;
    }

    private List<Map<String, Object>> getQuestionBank() {
        List<Map<String, Object>> bank = new ArrayList<>();

        Map<String, Object> q1 = new HashMap<>();
        q1.put("questionText", "根据《中华人民共和国民族团结进步促进法》第二条，民族团结进步事业坚持什么领导？");
        q1.put("options", Arrays.asList("中国共产党的全面领导", "多党合作领导", "民族自治领导", "社会团体领导"));
        q1.put("answer", "A");
        q1.put("analysis", "根据《中华人民共和国民族团结进步促进法》第二条，民族团结进步事业坚持中国共产党的全面领导。");
        q1.put("type", 1);
        bank.add(q1);

        Map<String, Object> q2 = new HashMap<>();
        q2.put("questionText", "根据《中华人民共和国民族团结进步促进法》第三条，铸牢中华民族共同体意识应当引导各族人民树立哪四个与共？");
        q2.put("options", Arrays.asList("休戚与共、荣辱与共、生死与共、命运与共", "团结与共、奋斗与共、发展与共、繁荣与共", "平等与共、自由与共、公正与共、法治与共", "经济与共、政治与共、文化与共、生态与共"));
        q2.put("answer", "A");
        q2.put("analysis", "根据《中华人民共和国民族团结进步促进法》第三条，铸牢中华民族共同体意识应当引导各族人民牢固树立休戚与共、荣辱与共、生死与共、命运与共的共同体理念。");
        q2.put("type", 1);
        bank.add(q2);

        Map<String, Object> q3 = new HashMap<>();
        q3.put("questionText", "根据《中华人民共和国民族团结进步促进法》第五条，以下说法正确的是？");
        q3.put("options", Arrays.asList("各民族享有特殊权利", "中华人民共和国各民族一律平等", "少数民族可以不受法律约束", "各民族可以独立发展"));
        q3.put("answer", "B");
        q3.put("analysis", "根据《中华人民共和国民族团结进步促进法》第五条，中华人民共和国各民族一律平等。禁止对任何民族的歧视和压迫。");
        q3.put("type", 1);
        bank.add(q3);

        Map<String, Object> q4 = new HashMap<>();
        q4.put("questionText", "根据《中华人民共和国民族团结进步促进法》第八条，国家坚持和完善什么制度？");
        q4.put("options", Arrays.asList("人民代表大会制度", "民族区域自治制度", "多党合作制度", "基层群众自治制度"));
        q4.put("answer", "B");
        q4.put("analysis", "根据《中华人民共和国民族团结进步促进法》第八条，国家坚持和完善民族区域自治制度，维护国家统一和民族团结。");
        q4.put("type", 1);
        bank.add(q4);

        Map<String, Object> q5 = new HashMap<>();
        q5.put("questionText", "根据《中华人民共和国民族团结进步促进法》第十五条，国家全面推广普及什么？");
        q5.put("options", Arrays.asList("少数民族语言文字", "地方方言", "国家通用语言文字", "外语"));
        q5.put("answer", "C");
        q5.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十五条，国家全面推广普及国家通用语言文字。");
        q5.put("type", 1);
        bank.add(q5);

        Map<String, Object> q6 = new HashMap<>();
        q6.put("questionText", "根据《中华人民共和国民族团结进步促进法》第六条，中华民族共同体意识是民族团结的什么？");
        q6.put("options", Arrays.asList("根本保障", "重要基础", "生命线", "之本"));
        q6.put("answer", "D");
        q6.put("analysis", "根据《中华人民共和国民族团结进步促进法》第六条，中华民族共同体意识是民族团结之本。");
        q6.put("type", 1);
        bank.add(q6);

        Map<String, Object> q7 = new HashMap<>();
        q7.put("questionText", "根据《中华人民共和国民族团结进步促进法》第四十条，国家保护什么自由？");
        q7.put("options", Arrays.asList("言论自由", "信仰自由", "婚姻自由", "迁徙自由"));
        q7.put("answer", "C");
        q7.put("analysis", "根据《中华人民共和国民族团结进步促进法》第四十条，国家保护公民婚姻自由。任何组织和个人不得以民族身份、风俗习惯、宗教信仰等为由干涉婚姻自由。");
        q7.put("type", 1);
        bank.add(q7);

        Map<String, Object> q8 = new HashMap<>();
        q8.put("questionText", "《中华人民共和国民族团结进步促进法》自何时起施行？");
        q8.put("options", Arrays.asList("2026年3月12日", "2026年7月1日", "2026年10月1日", "2027年1月1日"));
        q8.put("answer", "B");
        q8.put("analysis", "根据《中华人民共和国民族团结进步促进法》第六十五条，本法自2026年7月1日起施行。");
        q8.put("type", 1);
        bank.add(q8);

        Map<String, Object> q9 = new HashMap<>();
        q9.put("questionText", "根据《中华人民共和国民族团结进步促进法》第一条，本法的立法依据是什么？");
        q9.put("options", Arrays.asList("民法典", "刑法", "宪法", "行政法"));
        q9.put("answer", "C");
        q9.put("analysis", "根据《中华人民共和国民族团结进步促进法》第一条，根据宪法，制定本法。");
        q9.put("type", 1);
        bank.add(q9);

        Map<String, Object> q10 = new HashMap<>();
        q10.put("questionText", "根据《中华人民共和国民族团结进步促进法》第十一条，国家坚持以什么为引领？");
        q10.put("options", Arrays.asList("社会主义核心价值观", "个人主义", "自由主义", "功利主义"));
        q10.put("answer", "A");
        q10.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十一条，国家坚持以社会主义核心价值观为引领。");
        q10.put("type", 1);
        bank.add(q10);

        Map<String, Object> q11 = new HashMap<>();
        q11.put("questionText", "根据《中华人民共和国民族团结进步促进法》第四条，推进中华民族共同体建设应当统筹哪些方面？（多选题）");
        q11.put("options", Arrays.asList("经济建设", "政治建设", "文化建设", "社会建设", "生态文明建设"));
        q11.put("answer", "ABCDE");
        q11.put("analysis", "根据《中华人民共和国民族团结进步促进法》第四条，推进中华民族共同体建设应当统筹经济建设、政治建设、文化建设、社会建设和生态文明建设五个方面。");
        q11.put("type", 2);
        bank.add(q11);

        Map<String, Object> q12 = new HashMap<>();
        q12.put("questionText", "根据《中华人民共和国民族团结进步促进法》第十一条，国家引导各族群众坚定哪些认同？（多选题）");
        q12.put("options", Arrays.asList("对伟大祖国的认同", "对中华民族的认同", "对中华文化的认同", "对中国共产党的认同", "对中国特色社会主义的认同"));
        q12.put("answer", "ABCDE");
        q12.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十一条，坚定对伟大祖国、中华民族、中华文化、中国共产党、中国特色社会主义的认同。");
        q12.put("type", 2);
        bank.add(q12);

        Map<String, Object> q13 = new HashMap<>();
        q13.put("questionText", "根据《中华人民共和国民族团结进步促进法》第五条和第六条，以下哪些属于被禁止的行为？（多选题）");
        q13.put("options", Arrays.asList("破坏民族团结", "制造民族分裂", "对任何民族的歧视", "对任何民族的压迫"));
        q13.put("answer", "ABCD");
        q13.put("analysis", "根据《中华人民共和国民族团结进步促进法》第五条、第六条，禁止对任何民族的歧视和压迫，禁止破坏民族团结和制造民族分裂的行为。");
        q13.put("type", 2);
        bank.add(q13);

        Map<String, Object> q14 = new HashMap<>();
        q14.put("questionText", "根据《中华人民共和国民族团结进步促进法》，国家全面推广普及国家通用语言文字的同时，还尊重和保障什么？（多选题）");
        q14.put("options", Arrays.asList("少数民族语言文字的学习", "少数民族语言文字的使用", "少数民族古籍的保护", "少数民族古籍的整理"));
        q14.put("answer", "ABCD");
        q14.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十五条，国家尊重和保障少数民族语言文字的学习和使用，支持少数民族古籍的保护、整理、研究和利用。");
        q14.put("type", 2);
        bank.add(q14);

        Map<String, Object> q15 = new HashMap<>();
        q15.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于教育相关的规定？（多选题）");
        q15.put("options", Arrays.asList("将铸牢中华民族共同体意识的要求贯穿教育全过程", "使用国家统编教材", "融入课堂教学、社会实践", "融入主题教育和网络教育"));
        q15.put("answer", "ABCD");
        q15.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十六条，将铸牢中华民族共同体意识的要求贯穿教育全过程，融入课堂教学、社会实践、主题教育和网络教育相结合的教育体系，使用国家统编教材。");
        q15.put("type", 2);
        bank.add(q15);

        Map<String, Object> q16 = new HashMap<>();
        q16.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于共同繁荣发展的内容？（多选题）");
        q16.put("options", Arrays.asList("加快民族地区高质量发展", "推进各民族共同富裕", "支持民族地区全面深化改革开放", "推动各民族共同迈向社会主义现代化"));
        q16.put("answer", "ABCD");
        q16.put("analysis", "根据《中华人民共和国民族团结进步促进法》第三十二条，支持民族地区全面深化改革开放、全面融入国家发展战略、提升自我发展能力，加快民族地区高质量发展，推进各民族共同富裕，推动各民族共同迈向社会主义现代化。");
        q16.put("type", 2);
        bank.add(q16);

        Map<String, Object> q17 = new HashMap<>();
        q17.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于法律责任的规定？（多选题）");
        q17.put("options", Arrays.asList("破坏民族团结进步的处罚", "就业歧视的处罚", "网络运营者未履行管理责任的处罚", "暴力恐怖活动的刑事责任"));
        q17.put("answer", "ABCD");
        q17.put("analysis", "根据《中华人民共和国民族团结进步促进法》第五十八条至第六十二条，破坏民族团结进步、就业歧视、网络运营者未履行管理责任、暴力恐怖活动等行为均需承担相应法律责任。");
        q17.put("type", 2);
        bank.add(q17);

        Map<String, Object> q18 = new HashMap<>();
        q18.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于保障与监督机制？（多选题）");
        q18.put("options", Arrays.asList("党委统一领导", "政府依法管理", "各部门通力合作", "全社会共同参与"));
        q18.put("answer", "ABCD");
        q18.put("analysis", "根据《中华人民共和国民族团结进步促进法》第四十一条，坚持和完善党委统一领导、政府依法管理、统一战线工作部门牵头协调、民族工作部门履职尽责、各部门通力合作、全社会共同参与的民族工作格局。");
        q18.put("type", 2);
        bank.add(q18);

        Map<String, Object> q19 = new HashMap<>();
        q19.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于构筑共有精神家园的内容？（多选题）");
        q19.put("options", Arrays.asList("发展社会主义先进文化", "弘扬革命文化", "传承中华优秀传统文化", "树立中华文化符号"));
        q19.put("answer", "ABCD");
        q19.put("analysis", "根据《中华人民共和国民族团结进步促进法》第十三条、第十四条，发展社会主义先进文化，弘扬革命文化，传承中华优秀传统文化，树立和突出各民族共有共享的中华文化符号和中华民族形象。");
        q19.put("type", 2);
        bank.add(q19);

        Map<String, Object> q20 = new HashMap<>();
        q20.put("questionText", "根据《中华人民共和国民族团结进步促进法》，以下哪些属于促进交往交流交融的措施？（多选题）");
        q20.put("options", Arrays.asList("推进互嵌式社区环境建设", "完善共居共学、共建共享的社会条件", "支持跨区域就业创业", "开展青少年跨区域交流活动"));
        q20.put("answer", "ABCD");
        q20.put("analysis", "根据《中华人民共和国民族团结进步促进法》第二十二条至第二十七条，推进互嵌式社区环境建设，完善共居共学、共建共享、共事共乐的社会条件，支持跨区域就业创业，开展青少年跨区域交流活动。");
        q20.put("type", 2);
        bank.add(q20);

        return bank;
    }

    private Map<String, Object> generateGenericQuestion(Integer questionType) {
        Map<String, Object> question = new HashMap<>();
        question.put("questionText", "根据《中华人民共和国民族团结进步促进法》，民族团结进步事业坚持什么领导？");
        question.put("options", Arrays.asList("中国共产党的全面领导", "多党合作领导", "民族自治领导", "社会团体领导"));
        question.put("answer", "A");
        question.put("analysis", "根据《中华人民共和国民族团结进步促进法》第二条，民族团结进步事业坚持中国共产党的全面领导。");
        question.put("type", questionType);
        question.put("questionType", questionType);
        return question;
    }

    // ==================== RAG检索增强问答 ====================
    @Override
    public Object askRAG(Long userId, String question) {
        try {
            System.out.println("[RAG] 收到问题: " + (question != null ? question.substring(0, Math.min(30, question.length())) : "null"));
            // 1. 检索相关法条
            MatchResult matchResult = findRelatedContent(question);
            // 2. 构建RAG提示词（检索结果注入）
            String systemPrompt = buildRAGPrompt(matchResult);
            // 3. 调用DeepSeek
            String aiAnswer = callDeepSeek(systemPrompt, question);
            if (aiAnswer != null && !aiAnswer.startsWith("抱歉") && !aiAnswer.startsWith("调用AI服务失败")) {
                saveChat(userId, "[RAG]" + question, aiAnswer);
                return Result.success("【RAG检索增强回答】\n\n" + aiAnswer);
            }
            // 降级到本地知识库
            return Result.success("【RAG本地回答】\n\n" + generateLocalAnswer(question));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success("【RAG本地回答】\n\n" + generateLocalAnswer(question));
        }
    }

    private String buildRAGPrompt(MatchResult matchResult) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是法律AI助手，请仅基于以下检索到的法条原文和立法解读回答问题。\n\n");
        prompt.append("【回答规则】\n");
        prompt.append("1. 必须引用检索到的具体法条编号\n");
        prompt.append("2. 区分法条原文和立法解读\n");
        prompt.append("3. 如果检索材料不足以回答，请诚实说明\n\n");
        prompt.append("【检索到的相关法条】\n");
        if (!matchResult.articleKeys.isEmpty()) {
            for (String key : matchResult.articleKeys) {
                String content = getArticleContent(key);
                if (content != null) prompt.append("「").append(key).append("」").append(content).append("\n");
            }
        } else {
            prompt.append("（未检索到直接匹配的法条，请基于法律总览知识谨慎回答）\n");
        }
        if (!matchResult.interpretationKeys.isEmpty()) {
            prompt.append("\n【检索到的相关解读】\n");
            for (String key : matchResult.interpretationKeys) {
                String interp = LAW_INTERPRETATION_DATABASE.get(key);
                if (interp != null) prompt.append(interp).append("\n");
            }
        }
        return prompt.toString();
    }

    // ==================== 微调模拟问答 ====================
    @Override
    public Object askFineTuned(Long userId, String question) {
        try {
            System.out.println("[微调] 收到问题: " + (question != null ? question.substring(0, Math.min(30, question.length())) : "null"));
            // 构建微调模拟提示词（注入全部法律知识作为领域知识）
            String systemPrompt = buildFineTunedPrompt(question);
            String aiAnswer = callDeepSeek(systemPrompt, question);
            if (aiAnswer != null && !aiAnswer.startsWith("抱歉") && !aiAnswer.startsWith("调用AI服务失败")) {
                saveChat(userId, "[微调]" + question, aiAnswer);
                return Result.success("【微调模型回答】\n\n" + aiAnswer);
            }
            return Result.success("【微调模型回答】\n\n" + generateLocalAnswer(question));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success("【微调模型回答】\n\n" + generateLocalAnswer(question));
        }
    }

    private String buildFineTunedPrompt(String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是经过《中华人民共和国民族团结进步促进法》领域微调训练的法律专家模型。\n");
        prompt.append("你已完整学习该法律的全部65条法条及官方立法解读，能够像真正的法律专家一样回答问题。\n\n");
        prompt.append("【回答规则】\n");
        prompt.append("1. 你必须展现微调后的领域专业性——使用精确的法律术语\n");
        prompt.append("2. 每个回答必须引用具体法条编号和原文\n");
        prompt.append("3. 结合立法背景和目的进行深度分析\n");
        prompt.append("4. 给出实际应用场景和法律建议\n");
        prompt.append("5. 回答结构：核心结论 → 法条依据 → 深度解读 → 实际应用\n\n");
        // 注入完整法律知识库
        String fullArticles = loadAllArticlesForPrompt();
        prompt.append("【你的领域知识库 —— 已通过微调内化】\n");
        prompt.append(fullArticles);
        if (prompt.length() > 15000) {
            prompt.setLength(15000);
        }
        return prompt.toString();
    }

    private void saveChat(Long userId, String question, String answer) {
        try {
            AiChat chat = new AiChat();
            chat.setUserId(userId != null ? userId : 1L);
            chat.setQuestion(question);
            chat.setAnswer(answer);
            chat.setCreateTime(new Date());
            aiChatMapper.insert(chat);
        } catch (Exception e) {
            System.err.println("[AI] 保存失败: " + e.getMessage());
        }
    }
}
