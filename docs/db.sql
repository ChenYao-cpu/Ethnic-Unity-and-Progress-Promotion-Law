CREATE DATABASE law_study_system DEFAULT CHARACTER SET utf8mb4;

USE law_study_system;

CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(30) DEFAULT 'party_member' COMMENT 'party_member/admin/legal_officer',
    school VARCHAR(100) COMMENT '学校/单位',
    identity VARCHAR(300) COMMENT '身份JSON',
    total_score INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE assignment_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL COMMENT '作业ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    sort_order INT DEFAULT 0 COMMENT '题目排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_assignment_question (assignment_id, question_id)
);
CREATE TABLE assignment_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL COMMENT '作业ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    score INT DEFAULT 0 COMMENT '得分',
    submit_time DATETIME NOT NULL COMMENT '提交时间',
    answers TEXT COMMENT '答案JSON',
    status INT DEFAULT 1 COMMENT '状态：1-已完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_assignment_student (assignment_id, student_id)
) COMMENT='作业提交记录表';

CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    video_url VARCHAR(500),
    content TEXT,
    duration INT COMMENT '视频时长(分钟)',
    cover VARCHAR(255),
    category VARCHAR(50) COMMENT '课程分类：必修/民族/法规/政策',
    points INT DEFAULT 10 COMMENT '完成奖励积分',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE study_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    study_duration INT DEFAULT 0 COMMENT '学习时长(分钟)',
    progress INT DEFAULT 0 COMMENT '进度百分比',
    current_position INT DEFAULT 0 COMMENT '当前播放位置(秒)',
    status TINYINT DEFAULT 0 COMMENT '0未学习 1学习中 2已完成',
    study_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_course (user_id, course_id)
);

CREATE TABLE knowledge_tree (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    level INT DEFAULT 1 COMMENT '树的等级',
    growth_value INT DEFAULT 0 COMMENT '成长值',
    last_feed_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type TINYINT NOT NULL COMMENT '1单选 2多选 3判断 4情景',
    title TEXT NOT NULL,
    options JSON COMMENT '选项',
    answer VARCHAR(100) NOT NULL,
    analysis TEXT,
    difficulty TINYINT DEFAULT 1 COMMENT '1简单 2中等 3困难',
    image VARCHAR(500) COMMENT '题目配图URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer VARCHAR(100),
    is_correct TINYINT COMMENT '1正确 0错误',
    answer_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL COMMENT '试卷标题',
    description TEXT COMMENT '试卷描述',
    teacher_id BIGINT COMMENT '教师ID',
    single_count INT DEFAULT 0 COMMENT '单选题数量',
    multi_count INT DEFAULT 0 COMMENT '多选题数量',
    question_count INT DEFAULT 0 COMMENT '总题数',
    total_score INT DEFAULT 100 COMMENT '总分',
    generate_type TINYINT DEFAULT 1 COMMENT '1题库 2AI生成',
    category VARCHAR(50) COMMENT '分类',
    difficulty TINYINT DEFAULT 2 COMMENT '1简单 2中等 3困难',
    start_time DATETIME COMMENT '开始时间',
    deadline DATETIME NOT NULL COMMENT '截止时间',
    status TINYINT DEFAULT 1 COMMENT '1:有效 0:撤回',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    wrong_count INT DEFAULT 1,
    last_wrong_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_question (user_id, question_id)
);

CREATE TABLE exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100),
    type VARCHAR(50) COMMENT '专项训练/综合模拟/错题重考',
    questions JSON NOT NULL COMMENT '题目ID列表',
    user_answers JSON COMMENT '用户答案',
    score INT,
    total_score INT,
    correct_count INT,
    time_used INT COMMENT '用时(秒)',
    status TINYINT DEFAULT 0 COMMENT '0进行中 1已完成',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    finish_time DATETIME
);

CREATE TABLE study_note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    title VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    type TINYINT DEFAULT 1 COMMENT '1公告 2通知',
    is_top TINYINT DEFAULT 0,
    publish_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE score_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    score INT NOT NULL,
    type VARCHAR(50) COMMENT 'course_complete/answer_correct/exam_pass',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE regulation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 插入示例课程数据
INSERT INTO course (title, description, video_url, cover, duration, category, points, status) VALUES
('宪法基础知识入门', '学习宪法的基本内容和核心精神，了解公民基本权利与义务', 'https://example.com/video1.mp4', 'https://example.com/cover1.jpg', 30, '必修', 20, 1),
('民族团结进步条例解读', '深入解读民族团结相关法规政策，促进各民族交往交流交融', 'https://example.com/video2.mp4', 'https://example.com/cover2.jpg', 25, '民族', 15, 1),
('民法典重点条款详解', '民法典关键条款详细讲解，包括物权、合同、人格权等编', 'https://example.com/video3.mp4', 'https://example.com/cover3.jpg', 40, '法规', 25, 1),
('最新教育政策解读', '2024年教育政策变化要点分析，帮助理解教育改革方向', 'https://example.com/video4.mp4', 'https://example.com/cover4.jpg', 20, '政策', 10, 1),
('刑法基本原则', '刑法基本原则概述，罪刑法定、适用平等等原则解析', 'https://example.com/video5.mp4', 'https://example.com/cover5.jpg', 35, '必修', 20, 1),
('民族区域自治法', '民族区域自治制度的法律依据和实施要点', 'https://example.com/video6.mp4', 'https://example.com/cover6.jpg', 28, '民族', 15, 1),
('劳动法实务指南', '劳动合同法、社会保险法等劳动法律法规实用解读', 'https://example.com/video7.mp4', 'https://example.com/cover7.jpg', 32, '法规', 20, 1),
('乡村振兴政策概览', '乡村振兴战略政策解读，了解农业农村现代化发展', 'https://example.com/video8.mp4', 'https://example.com/cover8.jpg', 22, '政策', 10, 1);

-- 第1步：执行建表语句（创建表结构）
CREATE TABLE `law_article` (
                               `id` INT PRIMARY KEY AUTO_INCREMENT,
                               `chapter` VARCHAR(100) COMMENT '章节名称',
                               `article_number` VARCHAR(20) NOT NULL COMMENT '条号，如：第一条、第十二条',
                               `content` TEXT NOT NULL COMMENT '法条内容',
                               `keywords` VARCHAR(500) COMMENT '关键词，用逗号分隔，用于检索',
                               `sort_order` INT DEFAULT 0 COMMENT '排序',
                               `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='民族团结进步促进法法条表';

-- 第2步：执行索引创建（加速查询）
CREATE INDEX idx_article_number ON law_article(article_number);
CREATE INDEX idx_keywords ON law_article(keywords);

-- 第3步：
-- 序言（作为一条特殊记录，article_number为'序言'）
INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
    ('序言', '序言', '中国是世界上历史最悠久的国家之一，中华民族是有着五千多年文明史的伟大民族。中国各族人民在长期交往交流交融中，共同开拓了祖国的辽阔疆域，共同缔造了统一的多民族国家，共同书写了辉煌的中国历史，共同创造了灿烂的中华文化，共同培育了伟大的民族精神，凝聚成血脉相融、信念相同、文化相通、经济相依、情感相亲的命运共同体。
一八四〇年以后，封建的中国逐渐变成半殖民地、半封建的国家。中国各族人民始终坚持国土不可分、国家不可乱、民族不可散、文明不可断的大一统信念，在救亡图存、共御外侮的英勇斗争中，实现了中华民族从自在到自觉的伟大转变。
中国共产党是中国工人阶级的先锋队，同时是中国人民和中华民族的先锋队，始终把为中国人民谋幸福、为中华民族谋复兴作为初心使命，团结带领全国各族人民实现了民族独立和人民解放，建立了中华人民共和国，确保各族人民真正获得平等政治权利、共同当家做主人，实现各民族共同团结奋斗、共同繁荣发展，创造性地走出了一条中国特色解决民族问题的正确道路，中华民族面貌发生了历史性巨变。
中国特色社会主义进入新时代，中国共产党坚持把马克思主义民族理论同中国民族问题具体实际相结合、同中华优秀传统文化相结合，把铸牢中华民族共同体意识作为党的民族工作主线、民族地区各项工作的主线，形成了中国共产党关于加强和改进民族工作的重要思想，开辟了马克思主义民族理论中国化时代化新境界，中华民族共同体建设取得历史性成就。
中华民族是各民族凝聚成的多元一体大家庭。民族团结是我国各族人民的生命线。实现中华民族伟大复兴是全体中华儿女的共同追求，维护国家统一、促进民族团结进步是全体中国人民的共同责任。
全国各族人民、一切国家机关和武装力量、各政党和各社会团体、各企业事业组织，都要依照宪法和法律，把铸牢中华民族共同体意识、推进中华民族共同体建设作为共同任务，着眼中华民族根本利益和整体利益，以增进共同性为方向，维护、巩固和发展平等团结互助和谐的社会主义民族关系，高质量推进民族团结进步事业，以中华民族大团结促进中国式现代化，为全面建设社会主义现代化国家、全面推进中华民族伟大复兴而团结奋斗。', '序言,历史,五个共同,中华民族,命运共同体,民族团结', 0);


INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第一章 总则', '第一条', '为了促进民族团结进步，铸牢中华民族共同体意识，推进中华民族共同体建设，推动实现中华民族伟大复兴，根据宪法，制定本法。', '立法目的,中华民族共同体意识,中华民族伟大复兴,宪法', 1),
                                                                                                 ('第一章 总则', '第二条', '民族团结进步事业坚持中国共产党的全面领导，高举中国特色社会主义伟大旗帜，坚持马克思列宁主义、毛泽东思想、邓小平理论、"三个代表"重要思想、科学发展观，全面贯彻习近平新时代中国特色社会主义思想，巩固各民族团结奋斗的共同思想政治基础，坚定不移走中国特色解决民族问题的正确道路，为强国建设、民族复兴凝聚力量。', '党的领导,中国特色社会主义,民族复兴,解决民族问题道路', 2),
                                                                                                 ('第一章 总则', '第三条', '铸牢中华民族共同体意识，应当引导各族人民牢固树立休戚与共、荣辱与共、生死与共、命运与共的共同体理念，增强中华民族凝聚力。', '四个与共,共同体理念,中华民族凝聚力', 3),
                                                                                                 ('第一章 总则', '第四条', '推进中华民族共同体建设，应当统筹经济、政治、文化、社会和生态文明建设，全面实现各民族共同繁荣发展，确保各族人民共同当家做主人，构筑中华民族共有精神家园，促进各民族全方位互嵌和广泛交往交流交融，共同守护人与自然和谐共生的生态家园，推动中华民族成为认同度更高、凝聚力更强的命运共同体。', '共同体建设,共同繁荣,共有精神家园,交往交流交融,生态家园,命运共同体', 4),
                                                                                                 ('第一章 总则', '第五条', '中华人民共和国公民在法律面前一律平等。中华人民共和国各民族一律平等。禁止对任何民族的歧视和压迫。', '法律面前人人平等,民族平等,禁止歧视,禁止压迫', 5),
                                                                                                 ('第一章 总则', '第六条', '中华民族共同体意识是民族团结之本。国家坚持增进共同性、尊重和包容差异性，促进各民族守望相助、和谐共处，维护中华民族大团结。禁止破坏民族团结和制造民族分裂的行为。', '中华民族共同体意识,民族团结,增进共同性,中华民族大团结,禁止破坏民族团结', 6),
                                                                                                 ('第一章 总则', '第七条', '国家推动各民族共同团结奋斗、共同繁荣发展，促进物质文明、政治文明、精神文明、社会文明、生态文明协调发展，全面推进中华民族发展进步。', '共同团结奋斗,共同繁荣发展,五个文明', 7),
                                                                                                 ('第一章 总则', '第八条', '国家坚持和完善民族区域自治制度，维护国家统一和民族团结。', '民族区域自治,国家统一,民族团结', 8),
                                                                                                 ('第一章 总则', '第九条', '国家坚持依法治理民族事务，依法保障各族群众合法权益，加强宪法法律宣传教育，增强各族群众的国家意识、公民意识、法治意识，维护社会主义法治的统一、尊严和权威，在法治轨道上推进民族事务治理体系和治理能力现代化。', '依法治理,法治意识,国家意识,公民意识,民族事务治理体系,治理能力现代化', 9),
                                                                                                 ('第一章 总则', '第十条', '中华人民共和国公民有维护国家统一和全国各民族团结的义务，应当维护国家主权、安全、发展利益。民族团结进步事业不受外部势力的干涉。坚决反对一切以民族、宗教、人权等借口对中华人民共和国实施污蔑抹黑、遏制打压、渗透破坏等行为。', '公民义务,国家统一,反干涉,反渗透', 10);

INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第二章 构筑共有精神家园', '第十一条', '国家坚持以社会主义核心价值观为引领，深化爱国主义、集体主义、社会主义教育，引导各族群众弘扬以爱国主义为核心的民族精神和以改革创新为核心的时代精神，坚定对伟大祖国、中华民族、中华文化、中国共产党、中国特色社会主义的认同。', '社会主义核心价值观,五个认同,爱国主义,民族精神', 11),
                                                                                                 ('第二章 构筑共有精神家园', '第十二条', '国家组织开展中国共产党史、新中国史、改革开放史、社会主义发展史、中华民族发展史宣传教育，引导各族群众牢固树立正确的国家观、历史观、民族观、文化观、宗教观。中华人民共和国公民应当增强国家观念，传承和弘扬爱国主义精神，维护国旗、国歌、国徽等国家象征和标志的尊严。', '五史教育,五个观念,爱国主义,国家象征', 12),
                                                                                                 ('第二章 构筑共有精神家园', '第十三条', '国家发展社会主义先进文化，弘扬革命文化，传承中华优秀传统文化，引导各族群众增进中华文化认同，增强中华文化自信。各民族优秀传统文化都是中华文化的组成部分。国家坚持以社会主义先进文化引领各民族优秀传统文化的创造性转化和创新性发展，支持开展中华优秀传统文化的宣传和推广。各级人民政府应当加强对反映中华民族共同体形成和发展的文物古迹、传统建筑、名城名镇名村、历史街区、传统村落以及非物质文化遗产等各类文化资源的发掘、保护和合理利用。', '中华文化认同,文化自信,传统文化,文物保护', 13),
                                                                                                 ('第二章 构筑共有精神家园', '第十四条', '国家树立和突出各民族共有共享的中华文化符号和中华民族形象，依托中华民族的文化和自然遗产等资源，推动中华文明标识体系的构建。各级人民政府应当鼓励和支持在公共设施、规划及建筑设计、景区展陈、地名命名和公众活动等方面，表现和展示中华文化符号和中华民族形象。中华人民共和国公民应当维护中华民族的形象，尊重中华民族形成和发展的历史，不得进行侮辱、贬损和亵渎。', '中华文化符号,中华民族形象,文化标识,形象维护', 14),
                                                                                                 ('第二章 构筑共有精神家园', '第十五条', '国家全面推广普及国家通用语言文字。任何组织和个人不得妨碍公民学习和使用国家通用语言文字。学校及其他教育机构以国家通用语言文字为基本的教育教学用语用字。国家推动学前儿童学会普通话、完成义务教育的青少年能够基本掌握国家通用语言文字。国家机关以国家通用语言文字为公务用语用字。依照有关法律规定需要使用少数民族语言文字发布文书的，应当同时提供国家通用语言文字版本和少数民族语言文字版本。国家机关、社会团体、企业事业组织和其他社会组织，在公共场合需要同时使用国家通用语言文字和少数民族语言文字的，应当在位置、顺序等方面突出国家通用语言文字。国家尊重和保障少数民族语言文字的学习和使用，推动少数民族语言文字的规范化、标准化和信息化建设，支持少数民族古籍的保护、整理、研究和利用。', '国家通用语言文字,普通话,少数民族语言文字,双语', 15),
                                                                                                 ('第二章 构筑共有精神家园', '第十六条', '各级各类学校及其他教育机构应当将铸牢中华民族共同体意识的要求贯穿教育全过程，融入课堂教学、社会实践、主题教育和网络教育相结合的教育体系。各级各类学校及其他教育机构应当按照国家有关规定使用国家统编教材。教育行政部门应当在教材的编写、审核中落实铸牢中华民族共同体意识的要求。国务院教育行政、民族工作等部门组织编写有关中华民族共同体的系列教材或者读本。', '共同体意识教育,统编教材,课程融入,教材编写', 16),
                                                                                                 ('第二章 构筑共有精神家园', '第十七条', '国家推动中国自主的中华民族共同体史料体系、话语体系、理论体系建设，支持高等学校、科研机构等单位加强对中华民族共同体重大基础性问题的研究，阐释中华民族和中华文明多元一体格局的历史与内涵，揭示中华民族形成和发展的道理、学理、哲理。国家支持中华民族共同体理论研究人才的培养，优化学科专业设置和布局，推进学科体系建设，加强中华民族共同体研究机构建设。国家支持中华民族共同体学术交流平台建设，推动中外学术界、民间团体和智库开展交流合作。', '三个体系建设,理论研究,多元一体,学科建设', 17),
                                                                                                 ('第二章 构筑共有精神家园', '第十八条', '国家将铸牢中华民族共同体意识教育纳入国民教育、干部教育、社会教育体系。各级人民政府应当充分利用中华优秀传统文化资源、红色资源和体现中国特色社会主义建设成就的各类资源，因地制宜采取多种形式，广泛开展铸牢中华民族共同体意识教育。', '三大教育体系,共同体意识教育,红色资源', 18),
                                                                                                 ('第二章 构筑共有精神家园', '第十九条', '报刊、广播、电视等新闻媒体和出版单位、网络服务提供者应当开展有关铸牢中华民族共同体意识、推进中华民族共同体建设的宣传报道、成就展示等工作。国家推进国际传播能力建设，支持开展对外人文交流，阐释中华民族历史和中华民族共同体理论，宣传中华民族共同体建设的实践和成就，促进世界更好了解和认识中华民族和中华文化，推动人类文明交流互鉴。', '媒体宣传,国际传播,对外交流,文明互鉴', 19),
                                                                                                 ('第二章 构筑共有精神家园', '第二十条', '各级人民政府应当推动将铸牢中华民族共同体意识的要求融入家庭、家教和家风建设。未成年人的父母或者其他监护人应当依法履行家庭教育责任，教育和引导未成年人热爱中国共产党、热爱祖国、热爱人民、热爱中华民族，树立中华民族一家亲的观念，不得向未成年人灌输不利于民族团结进步的观念。', '家庭家教家风,家庭教育,未成年人教育,中华民族一家亲', 20),
                                                                                                 ('第二章 构筑共有精神家园', '第二十一条', '国家支持香港特别行政区、澳门特别行政区开展中华民族历史、中华文化和国情教育，引导香港特别行政区同胞、澳门特别行政区同胞自觉维护国家主权、安全、发展利益。国家促进两岸经济文化交流合作，深化两岸各领域融合发展，增进台湾同胞对中华民族的归属感、认同感、荣誉感，推动两岸同胞共同传承弘扬中华文化，增强同属中华民族、同是中国人的认识。国家加强同海外侨胞的联系交流，支持海外侨胞弘扬中华文化、促进中外文化交流合作。', '港澳台,一国两制,两岸交流,海外侨胞', 21);

INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第三章 促进交往交流交融', '第二十二条', '县级以上人民政府应当统筹经济社会发展规划和公共资源配置，推进互嵌式社区环境建设，完善各族群众共居共学、共建共享、共事共乐的社会条件。', '互嵌式社区,六共,社区建设,社会条件', 22),
                                                                                                 ('第三章 促进交往交流交融', '第二十三条', '县级以上地方人民政府应当将铸牢中华民族共同体意识的要求纳入城市规划、建设、治理和服务全过程，因地制宜完善友好型、包容性、融合式的城市民族工作政策措施。地方各级人民政府应当根据当地实际情况，在城乡建设、人口管理、住房政策、就业创业、社会服务等方面采取具体措施，促进各民族团结融合。地方各级人民政府应当组织和引导各族群众共同参与社区建设、社区治理、社区活动，支持有关单位和组织提供社会服务，促进各族群众和谐融居。', '城市民族工作,融合式政策,社区参与,和谐融居', 23),
                                                                                                 ('第三章 促进交往交流交融', '第二十四条', '县级以上人民政府应当加强民族地区之间、民族地区与其他地区之间人口流动服务平台的共建和信息的共享，增强协作能力，提高跨区域政务服务的便利化水平。', '人口流动,服务平台,跨区域政务,信息共享', 24),
                                                                                                 ('第三章 促进交往交流交融', '第二十五条', '县级以上人民政府应当依法保障民族地区之间、民族地区与其他地区之间跨区域就业创业公民的合法权益，支持开展法律法规和政策、国家通用语言文字、职业技能等方面的培训，开展职业指导、职业介绍等服务。', '跨区域就业,就业权益,职业培训,劳动保障', 25),
                                                                                                 ('第三章 促进交往交流交融', '第二十六条', '国务院教育行政部门和省级人民政府应当支持民族地区和其他地区高等学校双向跨区域招生，加强人才培养的协作。国务院教育行政部门和县级以上地方人民政府应当鼓励和支持民族地区和其他地区教师开展交流。各级各类学校及其他教育机构应当结合学校和学生的特点，促进各族学生共同学习、共同生活、共同成长进步。', '跨区域招生,教师交流,各族学生共学,人才培养', 26),
                                                                                                 ('第三章 促进交往交流交融', '第二十七条', '各级人民政府应当支持学校、群团组织和其他社会组织，利用中华民族共同体形成和发展的历史文化资源，开展青少年跨区域社会实践、研学游学和参观考察等交流活动，增强民族自豪感和自信心。', '青少年交流,研学游学,社会实践,民族自豪感', 27),
                                                                                                 ('第三章 促进交往交流交融', '第二十八条', '各级人民政府应当鼓励和支持志愿者和志愿服务组织，在各类志愿服务活动中促进各族群众的交流合作和友爱互助。', '志愿服务,交流合作,友爱互助', 28),
                                                                                                 ('第三章 促进交往交流交融', '第二十九条', '国家增进各民族文化互鉴融通，鼓励各民族互相欣赏优秀传统文化、互相学习语言文字。各级人民政府应当支持文化工作者和有关单位，创作和展示具有中华文化底蕴、体现各民族交往交流交融的文艺作品。各级人民政府应当支持图书馆、博物馆、文化馆（站）、纪念馆、美术馆、科技馆、工人文化宫、青少年宫等公共文化服务单位，开展反映中华民族历史和国家繁荣发展等方面内容的展示和交流活动。各级人民政府应当依托中华民族丰富的文化、体育等各类资源，鼓励和支持举办各族群众喜闻乐见、共同参与的中华民族传统节日、民俗文化、体育赛事等交流活动。', '文化互鉴,文艺创作,公共文化服务,传统节日', 29),
                                                                                                 ('第三章 促进交往交流交融', '第三十条', '国家发挥旅游业在促进各民族交往交流交融方面的积极作用，推进文化和旅游深度融合发展，开发有利于弘扬中华文化的旅游线路和文化创意产品。县级以上人民政府民族工作部门应当会同文化和旅游、文物等部门，依托中华民族共同体理论和史料，规范展陈展示、讲解等内容。', '旅游促三交,文旅融合,展陈规范,文化创意', 30),
                                                                                                 ('第三章 促进交往交流交融', '第三十一条', '国家支持利用互联网、大数据、人工智能等现代技术手段开展交流活动，鼓励和引导网络产品、服务的提供者制作、传播体现中华民族大团结的作品和信息，营造有利于铸牢中华民族共同体意识的和谐网络环境。任何组织和个人不得以文字、图片和音视频等方式，制作和传播含有民族仇恨、民族歧视等破坏民族团结进步内容的信息。网络运营者应当加强对其用户发布的信息的管理，发现含有前款规定的破坏民族团结进步内容的信息的，应当依法立即停止传输该信息，采取消除等处置措施，防止信息扩散，保存有关记录，并向有关主管部门报告。', '网络环境,技术赋能,民族团结内容,信息管理,禁止传播', 31);

INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第四章 推动共同繁荣发展', '第三十二条', '国家贯彻新发展理念，支持民族地区全面深化改革开放、全面融入国家发展战略、提升自我发展能力，加快民族地区高质量发展，推进各民族共同富裕，推动各民族共同迈向社会主义现代化。', '新发展理念,高质量发展,共同富裕,现代化', 32),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十三条', '县级以上人民政府及其有关部门制定和实施经济社会发展领域的规划计划、政策措施，应当有利于铸牢中华民族共同体意识、推进中华民族共同体建设，有利于维护国家统一、反对分裂，有利于改善民生、凝聚人心。', '三个有利于,政策制定,改善民生,凝聚人心', 33),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十四条', '国家促进区域协调发展，完善区域一体化发展机制和差别化区域支持政策，健全对口支援、东西部协作等帮扶协作机制。国家支持民族地区结合区域主体功能定位，统筹发展和安全，在维护国家边疆安全、资源能源安全、粮食安全和生态安全等方面担起使命责任、充分发挥作用。国家支持民族地区发挥自身优势，服务构建以国内大循环为主体、国内国际双循环相互促进的新发展格局，深度融入高质量共建"一带一路"。', '区域协调,对口支援,东西部协作,边疆安全,一带一路', 34),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十五条', '国务院有关部门和县级以上地方人民政府应当加强交通、能源、水利、信息、物流等基础设施建设，推进民族地区与其他地区之间的互联互通。', '基础设施,互联互通,交通,能源,物流', 35),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十六条', '国家建设现代化产业体系，因地制宜发展新质生产力，有序推进区域间的产业合作和利益共享机制建设，支持民族地区在优化区域产业链和供应链布局、推进全国统一大市场建设中充分发挥作用。国务院有关部门和县级以上地方人民政府应当支持民族地区立足资源禀赋并利用现代科学技术，发展农林牧渔业、农副食品加工业、纺织业、文化旅游业，以及传统工艺、传统医药等特色优势产业，发展壮大新型农村集体经济，推进乡村全面振兴和城乡融合发展。', '现代化产业,新质生产力,特色产业,乡村振兴', 36),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十七条', '国务院有关部门和县级以上地方人民政府应当推动就业、教育、医疗等方面公共服务资源的合理配置，增强基本公共服务均衡性和可及性，促进民族地区各级各类教育协调发展，提高民族地区公共服务保障能力。', '公共服务,资源配置,教育医疗,均衡发展', 37),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十八条', '国务院有关部门和县级以上地方人民政府应当统筹优化农业、生态、城镇等各类空间布局，加强民族地区的生态环境保护和自然资源的可持续利用，引导各族群众共同维护中华民族永续发展的生态空间。', '空间布局,生态保护,可持续发展,生态空间', 38),
                                                                                                 ('第四章 推动共同繁荣发展', '第三十九条', '国务院有关部门和边境地区县级以上地方人民政府应当统筹推进兴边富民、稳边固边，鼓励支援边境地区建设，推进基础设施和公共服务设施建设，改善边境村落人居环境和生产生活条件。国务院有关部门和边境地区县级以上地方人民政府应当加强沿边城镇体系建设，健全开发开放政策，推进开发开放平台和产业平台设施建设，增强产业支撑能力，支持边境贸易，发展边境旅游，鼓励跨境经济合作。', '兴边富民,稳边固边,边境建设,开发开放', 39),
                                                                                                 ('第四章 推动共同繁荣发展', '第四十条', '国家加强时代新风新貌的培育，推进公民道德建设，开展群众性法治宣传教育和科学技术普及，推动移风易俗，倡导文明进步的新风尚，引导各族群众在生活交往、婚丧嫁娶等活动中自觉遵守法律法规、遵循公序良俗。国家保护公民婚姻自由。任何组织和个人不得以民族身份、风俗习惯、宗教信仰等为由干涉婚姻自由。', '移风易俗,公民道德,法治宣传,婚姻自由', 40);
INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第五章 保障与监督', '第四十一条', '坚持和完善党委统一领导、政府依法管理、统一战线工作部门牵头协调、民族工作部门履职尽责、各部门通力合作、全社会共同参与的民族工作格局，健全民族工作协调机制。中央统一战线工作部门、国务院民族工作部门负责全国铸牢中华民族共同体意识、促进民族团结进步等工作的统筹协调和督促落实。地方各级统一战线工作部门、民族工作部门负责本地区铸牢中华民族共同体意识、促进民族团结进步等工作的协调推动、组织实施和督促落实。', '民族工作格局,协调机制,统筹协调,督促落实', 41),
                                                                                                 ('第五章 保障与监督', '第四十二条', '中央和国家机关各部门、地方各机关各部门在各自职责范围内开展铸牢中华民族共同体意识、促进民族团结进步工作。各机关对在本单位内发生的破坏民族团结进步的行为应当及时予以制止。公职人员在履行职责和日常生活中，应当发挥铸牢中华民族共同体意识、促进民族团结进步的模范带头作用。', '机关职责,制止违法行为,公职人员,模范带头', 42),
                                                                                                 ('第五章 保障与监督', '第四十三条', '各级人民代表大会和县级以上人民代表大会常务委员会依照法定职权，在各项工作中落实铸牢中华民族共同体意识、推进中华民族共同体建设的要求。', '人大职责,法定职权,共同体意识落实', 43),
                                                                                                 ('第五章 保障与监督', '第四十四条', '工会、共产主义青年团、妇女联合会、工商业联合会、文学艺术界联合会、作家协会、科学技术协会、归国华侨联合会、台湾同胞联谊会、残疾人联合会和其他群团组织，应当发挥各自优势，面向所联系的领域开展铸牢中华民族共同体意识工作，团结所联系的群体为中华民族共同体建设贡献力量。', '群团组织,各自优势,共同体意识工作', 44),
                                                                                                 ('第五章 保障与监督', '第四十五条', '企业事业组织应当遵守社会公德，履行社会责任，将铸牢中华民族共同体意识的要求融入业务培训、文化建设等活动中，促进民族团结进步。行业协会、商会、学会和基金会等社会组织，应当将铸牢中华民族共同体意识的要求，体现在行业自律规范、职业道德准则或者组织章程中，结合业务工作促进民族团结进步。', '企业责任,社会组织,行业自律,职业道德', 45),
                                                                                                 ('第五章 保障与监督', '第四十六条', '宗教团体、宗教院校和宗教活动场所，应当开展铸牢中华民族共同体意识宣传教育，坚持我国宗教中国化方向，引导宗教与社会主义社会相适应，引导宗教教职人员、信教群众弘扬爱国主义传统，促进民族和睦、宗教和顺、社会和谐。', '宗教中国化,宗教团体,爱国主义,民族和睦', 46),
                                                                                                 ('第五章 保障与监督', '第四十七条', '基层人民政府应当对居民委员会、村民委员会开展铸牢中华民族共同体意识工作给予指导、支持和帮助。居民委员会、村民委员会应当推动在居民公约、村规民约中体现铸牢中华民族共同体意识的要求，支持和引导各族群众增进团结、互相尊重、互相帮助，促进共同进步。', '基层治理,居委会,村委会,村规民约', 47),
                                                                                                 ('第五章 保障与监督', '第四十八条', '军队依照本法和有关法律规定，结合国防活动开展铸牢中华民族共同体意识宣传教育，充分利用自身资源保障中华民族共同体建设，巩固和发展军政军民团结，维护国家统一和安全。', '军队职责,国防活动,军政军民团结', 48),
                                                                                                 ('第五章 保障与监督', '第四十九条', '国家将铸牢中华民族共同体意识的要求贯穿干部的培养、选拔、使用和管理全过程，加强民族地区干部队伍建设，重视培养和使用少数民族干部。国家支持民族地区专业技术人才和高技能人才的培养，推动民族地区之间、民族地区与其他地区之间的干部和人才交流，引导干部和人才向民族地区基层流动。', '干部培养,少数民族干部,人才交流,基层流动', 49),
                                                                                                 ('第五章 保障与监督', '第五十条', '县级以上人民政府应当将铸牢中华民族共同体意识、促进民族团结进步工作纳入国民经济和社会发展规划及年度计划，将相关工作经费列入预算。县级以上人民政府财政部门依法开展资金使用等情况的指导和监督。', '规划纳入,经费预算,财政监督', 50),
                                                                                                 ('第五章 保障与监督', '第五十一条', '国家将民族事务纳入共建共治共享的社会治理机制，加强组织协调和基层治理力量，强化科技支撑，提升社会治理效能。', '社会治理,共建共治共享,科技支撑,治理效能', 51),
                                                                                                 ('第五章 保障与监督', '第五十二条', '国务院有关部门和地方各级人民政府应当贯彻总体国家安全观，健全民族领域重大事项请示报告制度，加强对民族领域重大风险隐患的排查、识别、评估、预警和处置，提升防范化解风险的能力，维护国家安全和社会稳定。', '总体国家安全观,风险防范,请示报告,国家安全', 52),
                                                                                                 ('第五章 保障与监督', '第五十三条', '地方各级人民政府和有关机关应当加强矛盾纠纷预防和化解能力建设，发挥多元化纠纷解决机制的作用，准确认定纠纷的性质，依法处理和化解纠纷，维护民族团结。任何组织和个人不得以民族身份、风俗习惯、宗教信仰等为由，故意引发或者激化矛盾，扰乱公共秩序。', '矛盾化解,纠纷解决,民族团结,禁止激化矛盾', 53),
                                                                                                 ('第五章 保障与监督', '第五十四条', '公民对破坏民族团结进步的行为有权进行投诉和举报，对国家机关及其工作人员不履行或者不正确履行铸牢中华民族共同体意识、促进民族团结进步法定职责的行为有权进行检举。违反本法规定，破坏民族团结进步，损害国家利益或者社会公共利益的，人民检察院可以依法提起公益诉讼。', '投诉举报,公益诉讼,检察院职责,公民权利', 54),
                                                                                                 ('第五章 保障与监督', '第五十五条', '开展民族团结进步示范创建工作，应当突出铸牢中华民族共同体意识的要求。对在民族团结进步事业中做出突出贡献的集体和个人，按照国家有关规定给予表彰、奖励。', '示范创建工作,示范创建,表彰奖励,突出贡献', 55),
                                                                                                 ('第五章 保障与监督', '第五十六条', '每年9月的第四周为民族团结进步宣传周，国家通过多种形式集中开展铸牢中华民族共同体意识宣传教育活动。', '9月的第四周，九月的第四周，民族团结进步宣传周,宣传教育活动', 56);

INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第六章 法律责任', '第五十七条', '国家机关及其工作人员不履行或者不正确履行本法规定职责的，或者对本法第五十八条、第五十九条、第六十条、第六十一条规定的违法行为未及时予以制止并依法予以处理的，由其主管部门或者有关机关责令改正；造成不良后果或者影响的，对负有责任的领导人员和直接责任人员依法给予处分；构成犯罪的，依法追究刑事责任。', '渎职,不作为,处分,刑事责任', 57),
                                                                                                 ('第六章 法律责任', '第五十八条', '任何组织和个人违反本法有关规定，破坏民族团结进步的，由县级以上人民政府有关部门按照职责及时予以制止、责令改正，并依法给予处罚。构成违反治安管理行为的，由公安机关依法给予治安管理处罚；构成犯罪的，依法追究刑事责任。', '破坏民族团结,行政处罚,治安处罚,刑事责任', 58),
                                                                                                 ('第六章 法律责任', '第五十九条', '任何组织和个人以民族身份为由实施就业歧视、拒绝提供商品或者服务，或者实施法律法规禁止的其他歧视行为的，由县级以上民族工作、人力资源和社会保障、市场监管等有关部门按照职责责令改正；造成不良后果或者影响的，予以警告或者通报批评。法律法规另有处罚规定的，从其规定。', '通用语言文字,妨碍学习,教育行政,责令改正', 59),
                                                                                                 ('第六章 法律责任', '第六十条', '社会团体、企业事业组织和其他社会组织对在本单位内发生的破坏民族团结进步的行为，应当及时予以制止；未及时制止，造成不良后果或者影响的，由上级机关或者有关主管部门予以警告或者通报批评，并对直接负责的主管人员和其他直接责任人员依法追究法律责任。', '网络违法信息,民族仇恨,民族歧视,平台责任', 60),
                                                                                                 ('第六章 法律责任', '第六十一条', '网络运营者违反本法规定，未履行管理责任的，由网信、电信、公安、国家安全、新闻出版、广播电视等有关主管部门按照职责责令改正拒不改正或者情节严重的，依法给予处罚。', '网络运营者,管理责任,依法处罚', 61),
                                                                                                 ('第六章 法律责任', '第六十二条', '组织、策划、实施暴力恐怖活动、民族分裂活动或者宗教极端活动，构成犯罪的，依法追究刑事责任。煽动或者资助实施前款行为，构成犯罪的，依法追究刑事责任。', '暴力恐怖,民族分裂,宗教极端,煽动资助', 62),
                                                                                                 ('第六章 法律责任', '第六十三条', '中华人民共和国境外的组织和个人，针对中华人民共和国实施破坏民族团结进步、制造民族分裂行为的，依法追究法律责任。', '境外组织,境外个人,境外行为,法律责任', 63);

INSERT INTO `law_article` (`chapter`, `article_number`, `content`, `keywords`, `sort_order`) VALUES
                                                                                                 ('第七章 附则', '第六十四条', '省、自治区、直辖市和设区的市、自治州人民代表大会及其常务委员会，可以结合当地实际情况，制定促进民族团结进步的地方性法规。', '地方立法,地方性法规,省级,市级', 64),
                                                                                                 ('第七章 附则', '第六十五条', '本法自2026年7月1日起施行。', '施行日期,生效日期', 65);


SELECT COUNT(*) FROM law_article;


SELECT article_number, COUNT(*) as cnt
FROM law_article
GROUP BY article_number
HAVING cnt > 1;


DELETE t1 FROM law_article t1
                   INNER JOIN law_article t2
WHERE t1.article_number = t2.article_number
  AND t1.id > t2.id
  AND t1.chapter = '第三章 促进交往交流交融';
SELECT COUNT(*) FROM law_article;
-- 应该返回 66

-- ============================================
-- V2 迁移：角色系统升级（党员/管理员/法务人员）
-- 兼容 MySQL 5.x / 8.x 的迁移脚本
-- 请在数据库中逐条执行以下语句
-- ============================================

-- 1. 给 user 表添加新字段（使用存储过程兼容 MySQL 5.x）
-- 方式一：MySQL 8.0+ 直接执行
-- ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `role` VARCHAR(30) DEFAULT 'party_member' COMMENT 'party_member/admin/legal_officer';
-- ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `school` VARCHAR(100) COMMENT '学校/单位';
-- ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `identity` VARCHAR(300) COMMENT '身份JSON';

-- 方式二：MySQL 5.x 兼容（逐条执行，忽略"列已存在"的错误提示即可）
ALTER TABLE `user` ADD COLUMN `role` VARCHAR(30) DEFAULT 'party_member' COMMENT 'party_member/admin/legal_officer';
ALTER TABLE `user` ADD COLUMN `school` VARCHAR(100) COMMENT '学校/单位';
ALTER TABLE `user` ADD COLUMN `identity` VARCHAR(300) COMMENT '身份JSON';

-- 2. 给 assignment 表添加审核状态字段
ALTER TABLE `assignment` ADD COLUMN `review_status` VARCHAR(30) DEFAULT 'draft' COMMENT 'draft/pending_review/approved/rejected/published';
ALTER TABLE `assignment` ADD COLUMN `review_comment` TEXT COMMENT '审核意见（法务人员填写）';
ALTER TABLE `assignment` ADD COLUMN `question_review` JSON COMMENT '逐题审核结果JSON：[{questionId,status,comment}]';

-- 3. 将已有的旧角色迁移
UPDATE `user` SET `role` = 'party_member' WHERE `role` = 'student' OR `role` IS NULL OR `role` = '';
UPDATE `user` SET `role` = 'admin' WHERE `role` = 'teacher';

-- 4. question 表添加图片字段
ALTER TABLE `question` ADD COLUMN `image` VARCHAR(500) COMMENT '题目配图URL';

-- 5. 创建通知提醒表（管理员催促党员完成作业）
CREATE TABLE IF NOT EXISTS `reminder` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `assignment_id` BIGINT NOT NULL COMMENT '作业ID',
    `from_user_id` BIGINT NOT NULL COMMENT '发送者ID（管理员）',
    `to_user_id` BIGINT NOT NULL COMMENT '接收者ID（党员）',
    `message` VARCHAR(500) COMMENT '提醒内容',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读 0未读 1已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_to_user` (`to_user_id`),
    INDEX `idx_assignment` (`assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催促提醒表';