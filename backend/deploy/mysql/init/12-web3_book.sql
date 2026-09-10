-- 书籍阅读模块（数据来源 zh.wikisource.org 公有领域/公共版权）
CREATE TABLE IF NOT EXISTS `book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL,
  `author` VARCHAR(128) DEFAULT NULL,
  `dynasty` VARCHAR(64) DEFAULT NULL,
  `category` VARCHAR(64) DEFAULT NULL,
  `category_sub` VARCHAR(128) DEFAULT NULL,
  `description` TEXT,
  `cover` VARCHAR(512) DEFAULT NULL,
  `chapter_count` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `book_chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `book_id` BIGINT NOT NULL,
  `chapter_no` INT NOT NULL,
  `chapter_title` VARCHAR(200) NOT NULL,
  `content` MEDIUMTEXT,
  `word_count` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` INT DEFAULT 0,
  KEY `idx_book` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AUTO-GENERATED from zh.wikisource.org (Public Domain / 公有領域)
SET NAMES utf8mb4;

INSERT INTO book (title,author,dynasty,category,category_sub,description,chapter_count) VALUES ('道德經','老子','先秦','道家','王弼本主文','道德經 ，先秦老子所著，公版全文。','81');
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),1,'一章','道可道，非常道，名可名，非常名；
無名，天地之始，有名，萬物之母。
故常無欲，以觀其妙，
常有欲，以觀其徼；
此兩者，同出而異名，同謂之玄。玄之又玄，眾妙之門。',80);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),2,'二章','天下皆知美之為美，斯惡已。皆知善之為善，斯不善已。故有無相生，難易相成，長短相較，高下相傾，音聲相和，前後相隨。
是以聖人處無為之事，
行不言之教；萬物作焉而不辭，生而不有，為而不恃，
功成而弗居。
夫唯弗居，是以不去。',110);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),3,'三章','不尚賢，使民不爭；不貴難得之貨，使民不為盜；不見可欲，使民心不亂。
是以聖人之治，虛其心，實其腹，
弱其志，強其骨。
常使民無知無欲。
使夫智者不敢為也。
為無為，則無不治。',87);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),4,'四章','道沖而用之或不盈，淵兮似萬物之宗；挫其銳，解其紛，和其光，同其塵，湛兮似或存。吾不知誰之子，象帝之先。',51);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),5,'五章','天地不仁，以萬物為芻狗；
聖人不仁，以百姓為芻狗。
天地之間，其猶橐籥乎？虛而不屈，動而愈出。
多言數窮，不如守中。',58);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),6,'六章','谷神不死，是謂玄牝。玄牝之門，是謂天地根。緜緜若存，用之不勤。',31);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),7,'七章','天長地久。天地所以能長且久者，以其不自生，
故能長生。是以聖人後其身而身先；外其身而身存。非以其無私邪，故能成其私。',58);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),8,'八章','上善若水。水善利萬物而不爭，處眾人之所惡，
故幾於道。
居善地，心善淵，與善仁，言善信，正善治，事善能，動善時。夫唯不爭，故無尤。',65);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),9,'九章','持而盈之，不如其已；
揣而梲之，不可長保。
金玉滿堂，莫之能守；
富貴而驕，自遺其咎。
功遂身退，天之道。',53);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),10,'十章','載營魄抱一，能無離乎？
專氣致柔，能嬰兒乎？
滌除玄覽，能無疵乎？
愛國治民，能無知乎？
天門開闔，能為雌乎？
明白四達，能無為乎？
生之，
畜之。
生而不有，為而不恃，長而不宰，是謂玄德。',95);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),11,'十一章','三十輻共一轂，當其無，有車之用。
埏埴以為器，當其無，有器之用。鑿戶牖以為室，當其無，有室之用。故有之以為利，無之以為用。',61);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),12,'十二章','五色令人目盲，五音令人耳聾，五味令人口爽，馳騁畋獵令人心發狂，
難得之貨令人行妨。
是以聖人為腹不為目，故去彼取此。',58);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),13,'十三章','寵辱若驚，貴大患若身。何謂寵辱若驚？寵為下，得之若驚，失之若驚，是謂寵辱若驚。
何謂貴大患若身？
吾所以有大患者，為吾有身，
及吾無身，
吾有何患？故貴以身為天下，若可寄天下；
愛以身為天下，若可託天下。',102);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),14,'十四章','視之不見名曰夷，聽之不聞名曰希，搏之不得名曰微。此三者，不可致詰，故混而為一。
其上不皦，其下不昧。繩繩不可名，復歸於無物。是謂無狀之狀，無物之象，
是謂惚恍。
迎之不見其首，隨之不見其後。執古之道，以御今之有。
能知古始，是謂道紀。',117);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),15,'十五章','古之善為士者，微妙玄通，深不可識。夫唯不可識，故強為之容。豫焉若冬涉川，
猶兮若畏四鄰，
儼兮其若容，渙兮若冰之將釋，敦兮其若樸，曠兮其若谷，混兮其若濁。
孰能濁以靜之徐清？孰能安以久動之徐生？
保此道者不欲盈，
夫唯不盈，故能蔽不新成。',119);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),16,'十六章','致虛極，守靜篤。
萬物並作，吾以觀復。
夫物芸芸，各復歸其根。歸根曰靜，是謂復命。復命曰常，
知常曰明。不知常，妄作凶。
知常容，
容乃公，
公乃全，
全乃天，
天乃道，
道乃久，
沒身不殆。',96);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),17,'十七章','太上，下知有之，
其次，親而譽之，
其次，畏之，
其次，侮之。
信不足焉，有不信焉。
悠兮其貴言，功成事遂，百姓皆謂：我自然。',63);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),18,'十八章','大道廢，有仁義；
智慧出，有大偽；
六親不和，有孝慈；國家昏亂，有忠臣。',36);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),19,'十九章','絕聖棄智，民利百倍；絕仁棄義，民復孝慈；絕巧棄利，盜賊無有。此三者以為文不足，故令有所屬﹕見素抱樸，少私寡欲。',55);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),20,'二十章','絕學無憂，唯之與阿，相去幾何？善之與惡，相去若何？人之所畏，不可不畏。
荒兮其未央哉﹗
眾人熙熙，如享太牢，如春登臺。
我獨泊兮其未兆，如嬰兒之未孩；
儽儽兮若無所歸。
眾人皆有餘，而我獨若遺。
我愚人之心也哉﹗
沌沌兮，
俗人昭昭，
我獨昏昏。俗人察察，
我獨悶悶。澹兮其若海，
飂兮若無止。
眾人皆有以，
而我獨頑似鄙。
我獨異於人，而貴食母。',174);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),21,'二十一章','孔德之容，惟道是從。
道之為物，惟恍惟惚。
惚兮恍兮，其中有象；恍兮惚兮，其中有物。
窈兮冥兮，其中有精；
其精甚真，其中有信。
自今及古，其名不去，
以閱眾甫。
吾何以知眾甫之狀哉？以此。',95);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),22,'二十二章','曲則全，
枉則直，
窪則盈，
敝則新，
少則得，多則惑。
是以聖人抱一為天下式。
不自見故明，不自是故彰，不自伐故有功，不自矜故長。夫唯不爭，故天下莫能與之爭。古之所謂曲則全者，豈虛言哉！誠全而歸之。',100);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),23,'二十三章','希言自然。
故飄風不終朝，驟雨不終日。孰為此者？天地。天地尚不能久，而況於人乎？
故從事於道者，道者同於道，
德者同於德，
失者同於失。
同於道者，道亦樂得之；同於德者，德亦樂得之；同於失者，失亦樂得之。
信不足焉，有不信焉。',113);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),24,'二十四章','企者不立，
跨者不行，自見者不明，自是者不彰，自伐者無功，自矜者不長。其在道也，曰餘食贅行。
物或惡之，故有道者不處。',59);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),25,'二十五章','有物混成，先天地生。
寂兮寥兮，獨立而不改，
周行而不殆，可以為天下母。
吾不知其名，
字之曰道，
強為之名曰大。
大曰逝，
逝曰遠，遠曰反。
故道大，天大，地大，王亦大。
域中有四大，
而王居其一焉。
人法地，地法天，天法道，道法自然。',119);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),26,'二十六章','重為輕根，靜為躁君。
是以聖人終日行不離輜重。
雖有榮觀，燕處超然。
奈何萬乘之主，而以身輕天下？輕則失本，躁則失君。',59);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),27,'二十七章','善行無轍迹，
善言無瑕讁；
善數不用籌策；
善閉無關楗而不可開，善結無繩約而不可解。
是以聖人常善救人，故無棄人；
常善救物，故無棄物，是謂襲明。故善人者，不善人之師；
不善人者，善人之資。
不貴其師，不愛其資，雖智大迷，
是謂要妙。',117);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),28,'二十八章','知其雄，守其雌，為天下谿。為天下谿，常德不離，復歸於嬰兒。
知其白，守其黑，為天下式。
為天下式，常德不忒，
復歸於無極。
知其榮，守其辱，為天下谷，常德乃足，復歸於樸。
樸散則為器，聖人用之，則為官長，
故大制不割。',109);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),29,'二十九章','將欲取天下而為之，吾見其不得已。天下神器，
不可為也，為者敗之，執者失之。
故物或行或隨，或歔或吹。或強或羸，或挫或隳。是以聖人去甚，去奢，去泰。',73);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),30,'三十章','以道佐人主者，不以兵強天下。
其事好還。
師之所處，荊棘生焉。大軍之後，必有凶年。
善有果而已，不敢以取強。
果而勿矜，果而勿伐，果而勿驕。
果而不得已，果而勿強。
物壯則老，是謂不道，不道早已。',98);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),31,'三十一章','夫佳兵者，不祥之器，物或惡之，故有道者不處。君子居則貴左，用兵則貴右。兵者不祥之器，非君子之器，不得已而用之，恬淡為上。勝而不美，而美之者，是樂殺人。夫樂殺人者，則不可以得志於天下矣。吉事尚左，凶事尚右。偏將軍居左，上將軍居右，言以喪禮處之。殺人之眾，以哀悲泣之，戰勝，以喪禮處之。',141);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),32,'三十二章','道常無名，樸雖小，天下莫能臣也。侯王若能守之，萬物將自賓。
天地相合，以降甘露，民莫之令而自均。
始制有名，名亦既有，夫亦將知止，知止所以不殆。
譬道之在天下，猶川谷之於江海。',88);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),33,'三十三章','知人者智，自知者明。
勝人者有力，自勝者強。
知足者富。
強行者有志。
不失其所者久。
死而不亡者壽。',51);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),34,'三十四章','大道氾兮，其可左右。
萬物恃之而生而不辭，功成不名有。衣養萬物而不為主，常無欲，可名於小；
萬物歸焉而不為主，可名為大。
以其終不自為大，故能成其大。',75);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),35,'三十五章','執大象，天下往。
往而不害，安平太。
樂與餌，過客止。道之出口，淡乎其無味，視之不足見，聽之不足聞，用之不足既。',56);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),36,'三十六章','將欲歙之，必固張之；將欲弱之，必固強之；將欲廢之，必固興之；將欲奪之，必固與之。是謂微明。
柔弱勝剛強。魚不可脫於淵，國之利器不可以示人。',69);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),37,'三十七章','道常無為
而無不為。
侯王若能守之，萬物將自化。化而欲作，吾將鎮之以無名之樸。
無名之樸，夫亦將無欲。
不欲以靜，天下將自定。',63);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),38,'三十八章','上德不德，是以有德；下德不失德，是以無德。上德無為而無以為，下德為之而有以為。上仁為之而無以為，上義為之而有以為。上禮為之而莫之應， 則攘臂而扔之。故失道而後德，失德而後仁，失仁而後義，失義而後禮。夫禮者，忠信之薄，而亂之首。前識者，道之華，而愚之始。是以大丈夫處其厚，不居其薄；處其實，不居其華。故去彼取此。',155);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),39,'三十九章','昔之得一者，
天得一以清，地得一以寧，神得一以靈，谷得一以盈，萬物得一以生，侯王得一以為天下貞。其致之，
天無以清將恐裂，
地無以寧將恐發，神無以靈將恐歇，谷無以盈將恐竭，萬物無以生將恐滅，侯王無以貴高將恐蹶。故貴以賤為本，高以下為基。是以侯王自稱孤﹑寡﹑不穀。此非以賤為本邪？非乎？故致數輿無輿，不欲琭琭如玉，珞珞如石。',161);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),40,'四十章','反者道之動，
弱者道之用。
天下萬物生於有，有生於無。',27);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),41,'四十一章','上士聞道，勤而行之；
中士聞道，若存若亡；下士聞道，大笑之。不笑不足以為道。故建言有之﹕
明道若昧，
進道若退，
夷道若纇，
上德若谷，
大白若辱，
廣德若不足，
建德若偷，
質真若渝，
大方無隅，
大器晚成，
大音希聲，
大象無形，
道隱無名。夫唯道，善貸且成。',132);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),42,'四十二章','道生一，一生二，二生三，三生萬物。萬物負陰而抱陽，沖氣以為和。人之所惡，唯孤﹑寡﹑不穀，而王公以為稱。故物或損之而益，或益之而損。
人之所教，我亦教之。
強梁者不得其死，吾將以為教父。',92);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),43,'四十三章','天下之至柔，馳騁天下之至堅。
無有入無間，吾是以知無為之有益。
不言之教，無為之益，天下希及之。',48);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),44,'四十四章','名與身孰親？
身與貨孰多？
得與亡孰病？
是故甚愛必大費，多藏必厚亡，
知足不辱，知止不殆，可以長久。',51);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),45,'四十五章','大成若缺，其用不弊。
大盈若沖，其用不窮。
大直若屈，其用不居。
大巧若拙，其用不輟。
大辯若訥，其用不差。
躁勝寒，靜勝熱。清靜為天下正。',70);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),46,'四十六章','天下有道，卻走馬以糞。
天下無道，戎馬生於郊。
禍莫大於不知足；咎莫大於欲得。故知足之足，常足矣。',49);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),47,'四十七章','不出戶，知天下；不闚牖，見天道。
其出彌遠，其知彌少。
是以聖人不行而知，不見而名，
不為而成。',48);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),48,'四十八章','為學日益，
為道日損。
損之又損，以至於無為。無為而無不為。
取天下常以無事，
及其有事，
不足以取天下。',53);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),49,'四十九章','聖人無常心，以百姓心為心。
善者，吾善之；不善者，吾亦善之，
德善。
信者，吾信之；不信者，吾亦信之，德信。聖人在天下歙歙，為天下渾其心，
百姓皆注其耳目,
聖人皆孩之。',85);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),50,'五十章','出生入死。
生之徒，十有三；死之徒，十有三；人之生，動之死地，亦十有三。夫何故？以其生生之厚。蓋聞善攝生者，陸行不遇兕虎，入軍不被甲兵；兕無所投其角，虎無所措其爪，兵無所容其刃。夫何故？以其無死地。',99);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),51,'五十一章','道生之，德畜之，物形之，勢成之。
是以萬物莫不尊道而貴德。
道之尊，德之貴，夫莫之命而常自然。
故道生之，德畜之。長之育之，亭之毒之，蓋之覆之。
生而不有，為而不恃，
長而不宰。是謂玄德。',94);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),52,'五十二章','天下有始，以為天下母。
既得其母，以知其子，既知其子，復守其母，沒身不殆。
塞其兌，閉其門，
終身不勤。
開其兌，濟其事，終身不救。
見小曰明，守柔曰強。
用其光，
復歸其明，
無遺身殃，是為習常。',99);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),53,'五十三章','使我介然有知，行於大道，唯施是畏。
大道甚夷，而民好徑。
朝甚除，
田甚蕪，倉甚虛；
服文綵，帶利劍，厭飲食，財貨有餘；是為盜夸。非道也哉！',70);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),54,'五十四章','善建者不拔，
善抱者不脫，
子孫以祭祀不輟。
修之於身，其德乃真；修之於家，其德乃餘；
修之於鄉，其德乃長；修之於國，其德乃豐；修之於天下，其德乃普。故以身觀身，以家觀家，以鄉觀鄉，以國觀國，
以天下觀天下。
吾何以知天下然哉？以此。',117);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),55,'五十五章','含德之厚，比於赤子。蜂蠆虺蛇不螫，猛獸不據，攫鳥不搏。
骨弱筋柔而握固。
未知牝牡之合而全作，
精之至也。終日號而不嗄，
和之至也。知和曰常，
知常曰明。
益生曰祥。
心使氣曰強。
物壯則老，謂之不道，不道早已。',106);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),56,'五十六章','知者不言，
言者不知。
塞其兌，閉其門，挫其銳，
解其分，
和其光，
同其塵，
是謂玄同。故不可得而親，不可得而疏；
不可得而利，不可得而害；
不可得而貴，不可得而賤。
故為天下貴。',91);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),57,'五十七章','以正治國，以奇用兵，以無事取天下。
吾何以知其然哉？以此。天下多忌諱，而民彌貧；民多利器，國家滋昬；
人多伎巧，奇物滋起；
法令滋彰，盜賊多有。
故聖人云﹕「我無為而民自化，我好靜而民自正，我無事而民自富，我無欲而民自樸。」',112);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),58,'五十八章','其政悶悶，其民淳淳；
其政察察，其民缺缺。
禍兮福之所倚，福兮禍之所伏。孰知其極？其無正。
正復為奇，
善復為妖。
人之迷，其日固久。
是以聖人方而不割，
廉而不劌，
直而不肆，
光而不燿。',95);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),59,'五十九章','治人事天，莫若嗇。
夫唯嗇，是謂早服；
早服謂之重積德；
重積德則無不克，無不克則莫知其極；
莫知其極，可以有國；
有國之母，可以長久；
是謂深根固柢，長生久視之道。',83);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),60,'六十章','治大國，若烹小鮮。
以道莅天下，其鬼不神；
非其鬼不神，其神不傷人；
非其神不傷人，聖人亦不傷人。
夫兩不相傷，故德交歸焉。',62);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),61,'六十一章','大國者下流，
天下之交。
天下之牝，
牝常以靜勝牡，以靜為下。
故大國以下小國，
則取小國；
小國以下大國，則取大國。
故或下以取，或下而取。
大國不過欲兼畜人，小國不過欲入事人。夫兩者各得其所欲，大者宜為下。',105);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),62,'六十二章','道者萬物之奧。
善人之寶，
不善人之所保。
美言可以市，尊行可以加人。
人之不善，何棄之有？
故立天子，置三公，
雖有拱璧以先駟馬，不如坐進此道。
古之所以貴此道者何？不曰以求得，有罪以免邪？故為天下貴。',102);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),63,'六十三章','為無為，事無事，味無味。
大小多少，報怨以德。
圖難於其易，為大於其細；天下難事必作於易，天下大事必作於細。是以聖人終不為大，故能成其大。夫輕諾必寡信，多易必多難。是以聖人猶難之，
故終無難矣',96);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),64,'六十四章','其安易持，其未兆易謀。
其脆易泮，其微易散。
為之於未有，
治之於未亂。
合抱之木，生於毫末；九層之臺，起於累土；千里之行，始於足下。為者敗之，執者失之。
是以聖人無為故無敗，無執故無失。民之從事，常於幾成而敗之。
慎終如始，則無敗事。是以聖人欲不欲，不貴難得之貨；
學不學，復眾人之所過。
以輔萬物之自然，而不敢為。',159);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),65,'六十五章','古之善為道者，非以明民，將以愚之。
民之難治，以其智多。
故以智治國，國之賊，
不以智治國，國之福。知此兩者亦稽式。常知稽式，是謂玄德。玄德深矣，遠矣，
與物反矣，
然後乃至大順。',90);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),66,'六十六章','江海所以能為百谷王者，以其善下之，故能為百谷王。是以欲上民，必以言下之。欲先民，必以身後之。是以聖人處上而民不重，處前而民不害。是以天下樂推而不厭，以其不爭，故天下莫能與之爭。',88);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),67,'六十七章','天下皆謂我道大，似不肖。夫唯大，故似不肖。若肖，久矣其細也夫！
我有三寶，持而保之。一曰慈，二曰儉，三曰不敢為天下先。
慈故能勇，
儉故能廣，
不敢為天下先，故能成器長。
今舍慈且勇，
舍儉且廣，舍後且先，死矣！
夫慈以戰則勝，
以守則固。天將救之，以慈衛之。',130);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),68,'六十八章','善為士者不武，
善戰者不怒，
善勝敵者不與，
善用人者為之下，是謂不爭之德，是謂用人之力，
是謂配天古之極。',54);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),69,'六十九章','用兵有言﹕「吾不敢為主而為客，不敢進寸而退尺。」是謂行無行，
攘無臂，扔無敵，
執無兵。
禍莫大於輕敵，輕敵幾喪吾寶。
故抗兵相加，哀者勝矣。',71);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),70,'七十章','吾言甚易知，甚易行。天下莫能知，莫能行。
言有宗，事有君。
夫唯無知，是以不我知。
知我者希，則我者貴。
是以聖人被褐懷玉。',62);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),71,'七十一章','知不知上，不知知病。
夫唯病病，是以不病。聖人不病，以其病病，是以不病。',36);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),72,'七十二章','民不畏威，則大威至。無狎其所居，無厭其所生。
夫唯不厭，
是以不厭。
是以聖人自知不自見，
自愛不自貴；
故去彼取此。',59);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),73,'七十三章','勇於敢則殺，
勇於不敢則活。
此兩者，或利或害。
天之所惡，孰知其故？是以聖人猶難之。
天之道，不爭而善勝，
不言而善應，
不召而自來，
繟然而善謀。
天網恢恢，疏而不失。',86);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),74,'七十四章','民不畏死，奈何以死懼之？若使民常畏死，而為奇者，吾得執而殺之，孰敢？
常有司殺者殺，夫代司殺者殺，是謂代大匠斲。
夫代大匠斲者，希有不傷其手矣。',72);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),75,'七十五章','民之饑，以其上食稅之多，是以饑。民之難治，以其上之有為，是以難治。民之輕死，以其上求生之厚，是以輕死。夫唯無以生為者，是賢於貴生。',65);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),76,'七十六章','人之生也柔弱，其死也堅強。
萬物草木之生也柔脆，其死也枯槁。
故堅強者死之徒，柔弱者生之徒。
是以兵強則不勝，
木強則兵。
強大處下，
柔弱處上。',73);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),77,'七十七章','天之道，其猶張弓與？高者抑之，下者舉之；有餘者損之，不足者補之。天之道，損有餘而補不足。
人之道則不然，
損不足以奉有餘。
孰能有餘以奉天下？唯有道者。
是以聖人為而不恃，功成而不處，其不欲見賢。',98);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),78,'七十八章','天下莫柔弱於水，而攻堅強者，莫之能勝，其無以易之。
弱之勝強，柔之勝剛，天下莫不知莫能行。是以聖人云﹕「受國之垢，是謂社稷主；受國不祥，是為天下王。」正言若反。',80);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),79,'七十九章','和大怨，必有餘怨，
安可以為善？是以聖人執左契，
而不責於人。有德司契，
無德司徹。
天道無親，常與善人。',53);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),80,'八十章','小國寡民，
使有什伯之器而不用，
使民重死而不遠徙。
雖有舟輿，無所乘之；雖有甲兵，無所陳之。
使人復結繩而用之，甘其食，美其服，安其居，樂其俗。鄰國相望，雞犬之聲相聞，民至老死，不相往來。',95);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='道德經' ORDER BY id DESC LIMIT 1),81,'八十一章','信言不美，
美言不信。
善者不辯，辯者不善。
知者不博，
博者不知。
聖人不積，
既以為人己愈有，
既以與人己愈多。
天之道，利而不害；
聖人之道，為而不爭。',79);

INSERT INTO book (title,author,dynasty,category,category_sub,description,chapter_count) VALUES ('論語','孔子及弟子','先秦','儒家','四書之首','論語 ，先秦孔子及弟子所著，公版全文。','20');
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),1,'學而第一','*註疏

 ''''''一之一''''''
子曰：「學而時習之，不亦說乎？有朋自遠方來，不亦樂乎？人不知而不慍，不亦君子乎？」

 ''''''一之二''''''
有子曰：「其爲人也孝弟，而好犯上者，鮮矣；不好犯上，而好作亂者，未之有也。君子務本，本立而道生；孝弟也者，其爲仁之本與？」

 ''''''一之三''''''
子曰：「巧言令色，鮮矣仁。」

 ''''''一之四''''''
曾子曰：「吾日三省吾身：爲人謀而不忠乎？與朋友交而不信乎？傳不習乎？」

 ''''''一之五''''''
子曰：「道千乘之國，敬事而信，節用而愛人，使民以時。」

 ''''''一之六''''''
子曰：「弟子入則孝，出則弟，謹而信，汎愛眾，而親仁。行有餘力，則以學文。」

 ''''''一之七''''''
子夏曰：「賢賢易色；事父母，能竭其力；事君，能致其身；與朋友交，言而有信；雖曰未學，吾必謂之學矣。」

 ''''''一之八''''''
子曰：「君子不重則不威，學則不固。主忠信，無友不如己者，過則勿憚改。」

 ''''''一之九''''''
曾子曰：「愼終追遠，民德歸厚矣。」

 ''''''一之十''''''
子禽問於子貢曰：「夫子至於是邦也，必聞其政，求之與？抑與之與？」子貢曰：「夫子溫、良、恭、儉、讓以得之。夫子之求之也，其諸異乎人之求之與？」

 ''''''一之十一''''''
子曰：「父在觀其志，父沒觀其行。三年無改於父之道，可謂孝矣。」

 ''''''一之十二''''''
有子曰：「」

 ''''''一之十三''''''
有子曰：「信近於義，言可復也；恭近於禮，遠恥辱也。因不失其親，亦可宗也。」

 ''''''一之十四''''''
子曰：「君子食無求飽，居無求安，敏於事而愼於言，就有道而正焉：可謂好學。」

 ''''''一之十五''''''
子貢曰：「貧而無諂，富而無驕，何如？」子曰：「可也。未若，富而好禮者也」。子貢曰：「《詩》云：『如切如磋，如琢如磨。』其斯之謂與？」子曰：「賜也，始可與言《詩》已矣！。」

 ''''''一之十六''''''
子曰：「不患人之不己知，患不知人也。」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=1411635 -{zh-hans:汉语普通话朗读; zh-hant:中華民國國語朗讀]
 （[http://www.ximalaya.com/4228109/sound/1411635 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2016年7月23日的版本、由 白雲出岫 使用zh-hans:汉语普通话; zh-hant:中華民國國語錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
}- Category:有聲文獻

Category:香港中學文憑考試指定文言篇章',1149);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),2,'爲政第二','*註疏

 ''''''二之一''''''
子曰：「爲政以德，譬如北辰，居其所，而眾星共之。」

 ''''''二之二''''''
子曰：「詩三百，一言以蔽之，曰思無邪。」

 ''''''二之三''''''
子曰：「道之以政，齊之以刑，民免而無恥；道之以德，齊之以禮，有恥且格。」

 ''''''二之四''''''
子曰：「吾十有五而志于學；三十而立；四十而不惑；五十而知天命；六十而耳順；七十而從心所欲，不踰矩。」

 ''''''二之五''''''
孟懿子問孝。子曰：「無違。」樊遲御，子吿之曰：「孟孫問孝於我，我對曰：『無違。』」樊遲曰：「何謂也？」子曰：「生，事之以禮；死，葬之以禮，祭之以禮。」

 ''''''二之六''''''
孟武伯問孝。子曰：「父母，唯其疾之憂。」

 ''''''二之七''''''
子游問孝。子曰：「今之孝者，是謂能養。至於犬馬，皆能有養。不敬，何以別乎？」

 ''''''二之八''''''
子夏問孝。子曰：「色難。有事，弟子服其勞；有酒食，先生饌。曾是以爲孝乎？」

 ''''''二之九''''''
子曰：「吾與回言終日，不違如愚。退而省其私，亦足以發。回也不愚。」

 ''''''二之十''''''
子曰：「視其所以，觀其所由，察其所安，人焉廋哉！人焉廋哉！」

 ''''''二之十一''''''
子曰：「溫故而知新，可以爲師矣。」

 ''''''二之十二''''''
子曰：「君子不器。」

 ''''''二之十三''''''
子貢問君子。子曰：「先行其言，而後從之。」

 ''''''二之十四''''''
子曰：「君子周而不比，小人比而不周。」

 ''''''二之十五''''''
子曰：「學而不思則罔，思而不學則殆。」

 ''''''二之十六''''''
子曰：「攻乎異端，斯害也已。」

 ''''''二之十七''''''
子曰：「由，誨女知之乎！知之爲知之，不知爲不知，是知也。」

 ''''''二之十八''''''
子張學干祿。子曰：「多聞闕疑，愼言其餘，則寡尤；多見闕殆，愼行其餘，則寡悔。言寡尤，行寡悔，祿在其中矣。」

 ''''''二之十九''''''
哀公問曰：「何爲則民服？」孔子對曰：「擧直錯諸枉，則民服；擧枉錯諸直，則民不服。」

 ''''''二之二十''''''
季康子問：「使民敬忠以勸，如之何？」子曰：「臨之以莊，則敬；孝慈，則忠；擧善而教不能，則勸。」

 ''''''二之二一''''''
或謂孔子曰：「子奚不爲政？」子曰：「，奚其爲爲政？」

 ''''''二之二二''''''
子曰：「人而無信，不知其可也。大車無輗，小車無軏，其何以行之哉？」

 ''''''二之二三''''''
子張問：「十世可知也？」子曰：「殷因於夏禮，所損益可知也；周因於殷禮，所損益可知也；其或繼周者，雖百世可知也。」

 ''''''二之二四''''''
子曰：「非其鬼而祭之，諂也。見義不爲，無勇也。」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=1411636 -{zh-hans:汉语普通话朗读; zh-hant:中華民國國語朗讀]
 （[http://www.ximalaya.com/4228109/sound/1411636 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2016年7月23日的版本、由 白雲出岫 使用zh-hans:汉语普通话; zh-hant:中華民國國語錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
}- Category:有聲文獻

Category:香港中學文憑考試指定文言篇章',1433);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),3,'八佾第三','*註疏

 ''''''三之一''''''
孔子謂季氏：「八佾舞於庭。是可忍也，孰不可忍也！」

 ''''''三之二''''''
三家者，以雍徹。子曰：「『相維辟公，天子穆穆。』奚取於三家之堂？」

 ''''''三之三''''''
子曰：「人而不仁，如禮何？人而不仁，如樂何？」

 ''''''三之四''''''
林放問禮之本。子曰：「大哉問！禮，與其奢也，寧儉；喪，與其易也，寧戚。」

 ''''''三之五''''''
子曰：「夷狄之有君，不如諸夏之亡也。」

 ''''''三之六''''''
季氏旅於泰山。子謂冉有曰：「女弗能救與？」對曰：「不能。」子曰：「嗚呼！曾謂泰山不如林放乎？」

 ''''''三之七''''''
子曰：「君子無所爭，必也射乎！揖讓而升，下而飮，其爭也君子。」

 ''''''三之八''''''
子夏問曰：「『巧笑倩兮，美目盼兮，素以爲絢兮。』何謂也？」子曰：「繪事後素。」曰：「禮後乎？」子曰：「起予者商也，始可與言《詩》已矣。」

 ''''''三之九''''''
子曰：「夏禮，吾能言之，杞不足徵也；殷禮，吾能言之，宋不足徵也。文獻不足故也，足，則吾能徵之矣。」

 ''''''三之十''''''
子曰：「禘自既灌而往者，吾不欲觀之矣。」

 ''''''三之十一''''''
或問「禘」之說。子曰：「不知也。知其說者之於天下也，其如示諸斯乎？」指其掌。

 ''''''三之十二''''''
祭如在，祭神如神在。子曰：「吾不與祭，如不祭。」

 ''''''三之十三''''''
王孫賈問曰：「『與其媚於奧，寧媚於竈。』何謂也？」子曰：「不然。獲罪於天，無所禱也。」

 ''''''三之十四''''''
子曰：「周監於二代，郁郁乎文哉！吾從周。」

 ''''''三之十五''''''
子入太廟，每事問。或曰：「孰謂鄹人之子知禮乎？入太廟，每事問。」子聞之曰：「是禮也！」

 ''''''三之十六''''''
子曰：「射不主皮，爲力不同科，古之道也。」

 ''''''三之十七''''''
子貢欲去吿朔之餼羊。子曰：「賜也！爾愛其羊，我愛其禮。」

 ''''''三之十八''''''
子曰：「事君盡禮，人以爲諂也。」

 ''''''三之十九''''''
定公問：「君使臣，臣事君，如之何？」孔子對曰：「君使臣以禮，臣事君以忠。」

 ''''''三之二十''''''
子曰：「《關雎》，樂而不淫，哀而不傷。」

 ''''''三之二一''''''
哀公問社於宰我。宰我對曰：「夏后氏以松，殷人以柏，周人以栗。曰：『使民戰栗。』」子聞之，曰：「成事不說，遂事不諫，既往不咎。」

 ''''''三之二二''''''
子曰：「管仲之器小哉！」或曰：「管仲儉乎？」曰：「管氏有三歸，官事不攝，焉得儉？然則管仲知禮乎？」曰：「邦君樹塞門，管氏亦樹塞門。邦君爲兩君之好，有反坫，管氏亦有反坫。管氏而知禮，孰不知禮？」

 ''''''三之二三''''''
子語魯大師樂，曰：「樂其可知。始作，翕如也。從之，純如也，皦如也，繹如也。以成。」

 ''''''三之二四''''''
儀封人請見，曰：「君子之至於斯，吾未嘗不得見也。」從者見之。出曰：「二三子，何患於喪乎？天下之無道也久矣，天將以夫子爲木鐸。」

 ''''''三之二五''''''
子謂韶：「盡美矣，又盡善也。」謂武：「盡美矣，未盡善也。」

 ''''''三之二六''''''
子曰：「居上不寬，爲禮不敬，臨喪不哀，吾何以觀之哉！」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=1411637 -{zh-hans:汉语普通话; zh-hant:中華民國國語朗讀]
 （[http://www.ximalaya.com/4228109/sound/1411637 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2016年7月23日的版本、由 白雲出岫 使用zh-hans:汉语普通话; zh-hant:中華民國國語錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
}- Category:有聲文獻',1621);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),4,'里仁第四','*註疏

 ''''''四之一''''''
子曰：「里仁爲美。擇不處仁，焉得知？」

 ''''''四之二''''''
子曰：「不仁者，不可以久處約，不可以長處樂。仁者安仁，知者利仁。」

 ''''''四之三''''''
子曰：「惟仁者能好人，能惡人。」

 ''''''四之四''''''
子曰：「苟志於仁矣，無惡也。」

 ''''''四之五''''''
子曰：「富與貴，是人之所欲也，不以其道得之，不處也。貧與賤，是人之所惡也；不以其道得之，不去也。君子去仁，惡乎成名？君子無終食之閒違仁，造次必於是，zh:顚;zh-hant:顛;zh-hans:颠沛必於是。」

 ''''''四之六''''''
子曰：「我未見好仁者，惡不仁者。好仁者，無以尙之；惡不仁者，其爲仁矣。不使不仁者加乎其身。有能一日用其力於仁矣乎？我未見力不足者！蓋有之矣，我未之見也。」

 ''''''四之七''''''
子曰：「人之過也，各於其黨。觀過，斯知仁矣。」

 ''''''四之八''''''
子曰：「朝聞道，夕死可矣！」

 ''''''四之九''''''
子曰：「士志於道，而恥惡衣惡食者，未足與議也！」

 ''''''四之十''''''
子曰：「君子之於天下也，無適也，無莫也，義之與比。」

 ''''''四之十一''''''
子曰：「君子懷德，小人懷土；君子懷刑，小人懷惠。」

 ''''''四之十二''''''
子曰：「放於利而行，多怨。」

 ''''''四之十三''''''
子曰：「能以禮讓爲國乎，何有？不能以禮讓爲國，如禮何？」

 ''''''四之十四''''''
子曰：「不患無位，患所以立。不患莫己知，未爲可知也。」

 ''''''四之十五''''''
子曰：「參乎！吾道一以貫之。」曾子曰：「唯。」子出，門人問曰：「何謂也？」曾子曰：「夫子之道，忠恕而已矣！」

 ''''''四之十六''''''
子曰：「君子喻於義，小人喻於利。」

 ''''''四之十七''''''
子曰：「見賢思齊焉，見不賢而內自省也。」

 ''''''四之十八''''''
子曰：「事父母幾諫；見志不從，又敬而不違，勞而不怨。」

 ''''''四之十九''''''
子曰：「父母在，不遠遊；遊必有方。」

 ''''''四之二十''''''
子曰：「三年無改於父之道，可謂孝矣。」

 ''''''四之二一''''''
子曰：「父母之年，不可不知也。一則以喜，一則以懼。」

 ''''''四之二二''''''
子曰：「古者言之不出，恥躬之不逮也。」

 ''''''四之二三''''''
子曰：「以約失之者，鮮矣。」

 ''''''四之二四''''''
子曰：「君子欲訥於言而敏於行。」

 ''''''四之二五''''''
子曰：「德不孤，必有鄰。」

 ''''''四之二六''''''
子游曰：「事君數，斯辱矣。朋友數，斯疏矣。」',1067);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),5,'公冶長第五','*註疏

 ''''''五之一''''''
子謂公冶長，「可妻也；雖在縲之中，非其罪也。」以其子妻之。

 ''''''五之二''''''
子謂南容，「邦有道，不廢；邦無道，免於刑戮。」以其兄之子妻之。

 ''''''五之三''''''
子謂子賤：「君子哉若人！魯無君子者，斯焉取斯？」

 ''''''五之四''''''
子貢問曰：「賜也何如？」子曰：「女器也」。曰：「何器也？」曰：「瑚璉也。」

 ''''''五之五''''''
或曰：「雍也，仁而不佞。」子曰：「焉用佞？禦人以口給，屢憎於人。不知其仁；焉用佞？」

 ''''''五之六''''''
子使漆雕開仕。對曰：「吾斯之未能信。」子說。

 ''''''五之七''''''
子曰：「道不行，乘桴浮於海，從我者，其由與？」子路聞之喜。子曰：「由也，好勇過我，無所取材。」

 ''''''五之八''''''
孟武伯問：「子路仁乎？」子曰：「不知也。」又問，子曰：「由也，千乘之國，可使治其賦也；不知其仁也。」「求也何如？」子曰：「求也，千室之邑，百乘之家，可使爲之宰也；不知其仁也。」「赤也何如？」子曰：「赤也，束帶立於朝，可使與賓客言也；不知其仁也。」

 ''''''五之九''''''
子謂子貢曰：「女與回也孰愈？」對曰：「賜也何敢望回！回也聞一以知十，賜也聞一以知二。」子曰：「弗如也。吾與女，弗如也。」

 ''''''五之十''''''
宰予晝寢。子曰：「朽木不可雕也，糞土之牆，不可杇也；於予與何誅！」子曰：「始吾於人也，聽其言而信其行；今吾於人也，聽其言而觀其行；於予與改是。」

 ''''''五之十一''''''
子曰：「吾未見剛者。」或對曰：「申棖。」子曰：「棖也慾！焉得剛？」

 ''''''五之十二''''''
子貢曰：「我不欲人之加諸我也，吾亦欲無加諸人。」子曰：「賜也，非爾所及也！」

 ''''''五之十三''''''
子貢曰：「夫子之文章，可得而聞也；夫子之言性與天道，不可得而聞也。」

 ''''''五之十四''''''
子路有聞，未之能行，唯恐有聞。

 ''''''五之十五''''''
子貢問曰：「孔文子，何以謂之文也？」子曰：「敏而好學，不恥下問，是以謂之文也。」

 ''''''五之十六''''''
子謂子產：「有君子之道四焉：其行己也恭，其事上也敬，其養民也惠，其使民也義。」

 ''''''五之十七''''''
子曰：「晏平仲善與人交，久而敬之。」

 ''''''五之十八''''''
子曰：「臧文仲居蔡，山節藻梲。何如其知也？」

 ''''''五之十九''''''
子張問曰：「令尹子文，三仕爲令尹，無喜色；三已之，無慍色。舊令尹之政，必以吿新令尹。何如？」子曰：「忠矣。」曰：「仁矣乎？」曰：「未知，焉得仁？」「崔子弒齊君，陳文子有馬十乘，棄而違之，至於他邦，則曰：『猶吾大夫崔子也！』違之，之一邦，則又曰：『猶吾大夫崔子也！』違之。何如？」子曰：「淸矣。」曰：「仁矣乎？」曰：「未知，焉得仁？」

 ''''''五之二十''''''
季文子三思而後行。子聞之曰：「再，斯可矣！」

 ''''''五之二一''''''
子曰：「甯武子，邦有道則知；邦無道則愚。其知可及也，其愚不可及也。」

 ''''''五之二二''''''
子在陳，曰：「歸與！歸與！吾黨之小子狂簡，斐然成章，不知所以裁之。」

 ''''''五之二三''''''
子曰：「伯夷、叔齊，不念舊惡，怨是用希。」

 ''''''五之二四''''''
子曰：「孰謂微生高直？或乞醯焉，乞諸其鄰而與之。」

 ''''''五之二五''''''
子曰：「巧言、令色、足恭，左丘明恥之，丘亦恥之。匿怨而友其人，左丘明恥之，丘亦恥之。」

 ''''''五之二六''''''
顏淵、季路侍。子曰：「盍各言爾志？」子路曰：「願，與朋友共，敝之而無憾。」顏淵曰：「願無伐善，無施勞。」子路曰：「願聞子之志。」子曰：「老者安之，朋友信之，少者懷之。」

 ''''''五之二七''''''
子曰：「已矣乎！吾未見能見其過，而內自訟者也。」

 ''''''五之二八''''''
子曰：「十室之邑，必有忠信如丘者焉，不如丘之好學也。」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=12522939 漢語普通話朗讀]
 （[http://www.ximalaya.com/4228109/sound/12522939 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2016年7月23日的版本、由 白雲出岫 使用漢語普通話錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
 Category:有聲文獻',1844);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),6,'雍也第六','*註疏

 ''''''六之一''''''
子曰：「雍也，可使南面。」仲弓問子桑伯子。子曰：「可也，簡。」仲弓曰：「居敬而行簡，以臨其民，不亦可乎？居簡而行簡，無乃大簡乎？」子曰：「雍之言然。」

 ''''''六之二''''''
哀公問：「弟子孰爲好學？」孔子對曰：「有顏回者，好學；不遷怒，不貳過，不幸短命死矣！今也則亡，未聞好學者也。」

 ''''''六之三''''''
子華使於齊，冉子爲其母請粟。子曰：「與之釜。」請益，曰：「與之庾。」冉子與之粟五秉。子曰：「赤之適齊也，乘肥馬，衣輕裘；吾聞之也：君子周急不繼富。」原思爲之宰，與之粟九百，辭。子曰：「毋！以與爾鄰里鄉黨乎！」

 ''''''六之四''''''
子謂仲弓曰：「犁牛之子，騂且角；雖欲勿用，山川其舍諸？」

 ''''''六之五''''''
子曰：「回也，其心三月不違仁。其餘，則日月至焉而已矣。」

 ''''''六之六''''''
季康子問：「仲由可使從政也與？」子曰：「由也果，於從政乎何有！」曰：「賜也可使從政也與？」曰：「賜也達，於從政乎何有！」曰：「求也可使從政也與？」曰：「求也藝，於從政乎何有！」

 ''''''六之七''''''
季氏使閔子騫爲費宰。閔子騫曰：「善爲我辭焉。如有復我者，則吾必在汶上矣。」

 ''''''六之八''''''
伯牛有疾，子問之，自牖執其手，曰：「亡之，命矣夫！斯人也，而有斯疾也！斯人也，而有斯疾也！」

 ''''''六之九''''''
子曰：「賢哉回也！一簞食，一瓢飮，在陋巷，人不堪其憂，回也不改其樂。賢哉回也！」

 ''''''六之十''''''
冉求曰：「非不說子之道，力不足也。」子曰：「力不足者，中道而廢；今女畫。」

 ''''''六之十一''''''
子謂子夏曰：「女爲君子儒，無爲小人儒。」
 註解：「女」有「汝、你」的意思。本句文意是「孔子給學生子夏的期許，希望子夏能做大事，不做計算小事的儒者。」

 ''''''六之十二''''''
子游爲武城宰。子曰：「女得人焉耳乎？」曰：「有澹臺滅明者，行不由徑；非公事，未嘗至於偃之室也。」

 ''''''六之十三''''''
子曰：「孟之反不伐，奔而殿，將入門，策其馬，曰：『非敢後也，馬不進也。』」

 ''''''六之十四''''''
子曰：「不有祝鮀之佞，而有宋朝之美，難乎免於今之世矣。」

 ''''''六之十五''''''
子曰：「誰能出不由戶？何莫由斯道也！」

 ''''''六之十六''''''
子曰：「質勝文則野，文勝質則史。文質彬彬，然後君子。」

 ''''''六之十七''''''
子曰：「人之生也直，罔之生也幸而免。」

 ''''''六之十八''''''
子曰：「知之者，不如好之者，好之者，不如樂之者。」

 ''''''六之十九''''''
子曰：「中人以上，可以語上也；中人以下，不可以語上也。」

 ''''''六之二十''''''
樊遲問知。子曰：「務民之義，敬鬼神而遠之，可謂知矣。」問仁。曰：「仁者先難而後獲，可謂仁矣。」

 ''''''六之二一''''''
子曰：「知者樂水，仁者樂山。知者動，仁者靜。知者樂，仁者壽。」

 ''''''六之二二''''''
子曰：「齊一變，至於魯；魯一變，至於道。」

 ''''''六之二三''''''
子曰：「觚不觚，觚哉！觚哉！」

 ''''''六之二四''''''
宰我問曰：「仁者雖吿之曰：『井有仁焉。』其從之也？」子曰：「何爲其然也？君子可逝也，不可陷也。可欺也，不可罔也。」

 ''''''六之二五''''''
子曰：「君子博學於文，約之以禮，亦可以弗畔矣夫！」

 ''''''六之二六''''''
子見南子，子路不說。夫子矢之曰：「予所否者，天厭之！天厭之！」

 ''''''六之二七''''''
子曰：「中庸之爲德也，其至矣乎！民鮮久矣！」

 ''''''六之二八''''''
子貢曰：「如有博施於民，而能濟眾，何如？可謂仁乎？」子曰：「何事於仁，必也聖乎？堯舜其猶病諸！夫仁者，己欲立而立人，己欲達而達人。能近取譬，可謂仁之方也已。」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=1411640 漢語普通話朗讀]
 （[http://www.ximalaya.com/4228109/sound/1411640 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2017年7月7日的版本、由 白雲出岫 使用漢語普通話錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
 Category:有聲文獻',1807);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),7,'述而第七','*註疏
  ''''''七之一''''''
子曰：「述而不作，信而好古，竊比於我老彭。」

 ''''''七之二''''''
子曰：「默而識之，學而不厭，誨人不倦，何有於我哉？」

 ''''''七之三''''''
子曰：「德之不脩，學之不講，聞義不能徙，不善不能改，是吾憂也。」

 ''''''七之四''''''
子之燕居，申申如也，夭夭如也。

 ''''''七之五''''''
子曰：「甚矣，吾衰也！久矣，吾不復夢見周公！」

 ''''''七之六''''''
子曰：『志於道，狎於德，依於仁，游於藝。』

 ''''''七之七''''''
子曰：「自行束脩以上，吾未嘗無誨焉。」

 ''''''七之八''''''
子曰：「不憤不啟；不悱不發。，不以三隅反，。」

 ''''''七之九''''''
子食於有喪者之側，未嘗飽也。子於是日哭，則不歌。

 ''''''七之十''''''
子謂顏淵曰：「用之則行，舍之則藏。惟我與爾有是夫！」子路曰：「子行三軍，則誰與？」子曰：「暴虎馮河，死而無悔者，吾不與也。必也臨事而懼，好謀而成者也。」

 ''''''七之十一''''''
子曰：「富而可求也，雖執鞭之士，吾亦爲之；如不可求，從吾所好。」

 ''''''七之十二''''''
子之所愼：齊、戰、疾。

 ''''''七之十三''''''
子在齊聞韶，三月不知肉味，曰：「不圖爲樂之至於斯也！」

 ''''''七之十四''''''
冉有曰：「夫子爲衞君乎？」子貢曰：「諾，吾將問之」。入曰：「伯夷叔齊，何人也？」曰：「古之賢人也。」曰：「怨乎？」曰：「求仁而得仁，又何怨？」出，曰：「夫子不爲也。」

 ''''''七之十五''''''
子曰：「飯疏食，飮水，曲肱而枕之，樂亦在其中矣。不義而富且貴，於我如浮雲。」

 ''''''七之十六''''''
子曰：「加我數年，五十以學易，可以無大過矣。」

 ''''''七之十七''''''
子所雅言：「詩、書、執禮，皆雅言也。」

 ''''''七之十八''''''
葉公問孔子於子路，子路不對。子曰：「女奚不曰：『其爲人也，發憤忘食，樂以忘憂，不知老之將至云爾。』」

 ''''''七之十九''''''
子曰：「我非生而知之者，好古，敏以求之者也。」

 ''''''七之二十''''''
子不語：怪、力、亂、神。

 ''''''七之二一''''''
子曰：「三人行，必有我師焉。擇其善者而從之，其不善者而改之。」

 ''''''七之二二''''''
子曰：「天生德於予，桓魋其如予何！」

 ''''''七之二三''''''
子曰：「二三子，以我爲隱乎？吾無隱乎爾！吾無行而不與二三子者，是丘也。」

 ''''''七之二四''''''
子以四教：文、行、忠、信。

 ''''''七之二五''''''
子曰：「聖人，吾不得而見之矣！得見君子者，斯可矣。」子曰：「善人，吾不得而見之矣！得見有恆者，斯可矣。亡而爲有，虛而爲盈，約而爲泰，難乎有恆矣！」

 ''''''七之二六''''''
子釣而不綱，弋而不射宿。

 ''''''七之二七''''''
子曰：「蓋有不知而作之者，我無是也。多聞，擇其善者而從之，多見而識之，知之次也。」

 ''''''七之二八''''''
互鄉難與言。童子見，門人惑。子曰：「與其進也，不與其退也。唯何甚？人潔己以進，與其潔也，不保其往也！」

 ''''''七之二九''''''
子曰：「仁遠乎哉？我欲仁，斯仁至矣。」

 ''''''七之三十''''''
陳司敗問：「昭公知禮乎？」孔子對曰：「知禮。」孔子退，揖巫馬期而進之，曰：「吾聞君子不黨，君子亦黨乎？君取於吳爲同姓，謂之吳孟子。君而知禮，孰不知禮？」巫馬期以吿。子曰：「丘也幸，苟有過，人必知之。」

 ''''''七之三一''''''
子與人歌而善，必使反之，而後和之。

 ''''''七之三二''''''
子曰：「文，莫吾猶人也；躬行君子，則吾未之有得！」

 ''''''七之三三''''''
子曰：「若聖與仁，則吾豈敢？抑爲之不厭，誨人不倦，則可謂云爾已矣！」公西華曰：「正唯弟子不能學也！」

 ''''''七之三四''''''
子疾病，子路請禱。子曰：「有諸？」子路對曰：「有之。誄曰：『禱爾于上下神祇。』子曰：「丘之禱久矣！」

 ''''''七之三五''''''
子曰：「奢則不孫，儉則固；與其不孫也，甯固。」

 ''''''七之三六''''''
子曰：「君子坦蕩蕩，小人長戚戚。」

 ''''''七之三七''''''
子溫而厲，威而不猛，恭而安。',1700);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),8,'泰伯第八','*註疏
  ''''''八之一''''''
子曰：「泰伯，其可謂至德也已矣！三以天下讓，民無得而稱焉。」

 ''''''八之二''''''
子曰：「恭而無禮則勞，愼而無禮則葸，勇而無禮則亂，直而無禮則絞。君子篤於親，則民興於仁。故舊不遺，則民不偷。」

 ''''''八之三''''''
曾子有疾，召門弟子曰：「啟予足！啟予手！詩云：『戰戰兢兢，如臨深淵，如履薄冰。』而今而後，吾知免夫！小子！」

 ''''''八之四''''''
曾子有疾，孟敬子問之。曾子言曰：「鳥之將死，其鳴也哀，人之將死，其言也善。君子所貴乎道者三：動容貌，斯遠暴慢矣；正顏色，斯近信矣；出辭氣，斯遠鄙倍矣；籩豆之事，則有司存。」

 ''''''八之五''''''
曾子曰：「以能問於不能，以多問於寡，有若無，實若虛，犯而不校。昔者吾友，嘗從事於斯矣。」

 ''''''八之六''''''
曾子曰：「可以託六尺之孤，可以寄百里之命，臨大節而不可奪也。君子人與？君子人也！」

 ''''''八之七''''''
曾子曰：「士不可以不弘毅，任重而道遠。仁以爲己任，不亦重乎；死而後已，不亦遠乎。」

 ''''''八之八''''''
子曰：「興於詩，立於禮，成於樂。」

 ''''''八之九''''''
子曰：「民可使由之，不可使知之。」

 ''''''八之十''''''
子曰：「好勇疾貧，亂也。人而不仁，疾之已甚，亂也。」

 ''''''八之十一''''''
子曰：「如有周公之才之美，使驕且吝，其餘不足觀也已！」

 ''''''八之十二''''''
子曰：「三年學，不至於穀，不易得也。」

 ''''''八之十三''''''
子曰：「篤信好學，守死善道。危邦不入，亂邦不居。天下有道則見，無道則隱。邦有道，貧且賤焉，恥也；邦無道，富且貴焉，恥也。」

 ''''''八之十四''''''
子曰：「不在其位，不謀其政。」

 ''''''八之十五''''''
子曰：「師摯之始，關雎之亂，洋洋乎，盈耳哉！」

 ''''''八之十六''''''
子曰：「狂而不直，侗而不愿，悾悾而不信，吾不知之矣！」

 ''''''八之十七''''''
子曰：「學如不及，猶恐失之。」

 ''''''八之十八''''''
子曰：「巍巍乎，舜、禹之有天下也，而不與焉。」

 ''''''八之十九''''''
子曰：「大哉，堯之爲君也！巍巍乎，唯天爲大，唯堯則之！蕩蕩乎，民無能名焉！巍巍乎，其有成功也！煥乎，其有文章！」

 ''''''八之二十''''''
舜有臣五人，而天下治。武王曰：「予有亂臣十人。」孔子曰：「『才難』，不其然乎？唐虞之際，於斯爲盛，有婦人焉，九人而已。三分天下有其二，以服事殷、周之德，其可謂至德也已矣！」

 ''''''八之二一''''''
子曰：「禹，吾無間然矣！菲飮食，而致孝乎鬼神；惡衣服，而致美乎黻冕；卑宮室，而盡力乎溝洫。禹，吾無間然矣！」

----',1110);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),9,'子罕第九','*註疏

  ''''''九之一''''''
子罕言利與命與仁。

 ''''''九之二''''''
達巷黨人曰：「大哉孔子！博學而無所成名。」子聞之，謂門弟子曰：「吾何執？執御乎？執射乎？吾執御矣！」

 ''''''九之三''''''
子曰：「麻冕，禮也；今也純，儉，吾從眾。拜下，禮也；今拜乎上，泰也。雖違眾，吾從下。」

 ''''''九之四''''''
子絕四：「毋意，毋必，毋固，毋我。」

 ''''''九之五''''''
子畏於匡。曰：「文王既沒，文不在茲乎？天之將喪斯文也，後死者，不得與於斯文也。天之未喪斯文也，匡人其如予何？」

 ''''''九之六''''''
大宰問於子貢曰：「夫子聖者與？何其多能也？」子貢曰：「固天縱之將聖，又多能也。」子聞之曰：「大宰知我乎！吾少也賤，故多能鄙事。君子多乎哉？不多也！」

 ''''''九之七''''''
牢曰：「子云：『吾不試，故藝。』」

 ''''''九之八''''''
子曰：「吾有知乎哉？無知也。有鄙夫問於我，空空如也，我扣其兩端而竭焉。」

 ''''''九之九''''''
子曰：「鳳鳥不至，河不出圖，吾已矣夫！」

 ''''''九之十''''''
子見齊衰者，冕衣裳者，與瞽者，見之，雖少必作，過之必趨。

 ''''''九之十一''''''
顏淵喟然歎曰：「仰之彌高，鑽之彌堅，瞻之在前，忽焉在後！夫子循循然善誘人：博我以文，約我以禮。欲罷不能，既竭吾才，如有所立卓爾，雖欲從之，末由也已！」

 ''''''九之十二''''''
子疾病，子路使門人爲臣。病間，曰：「久矣哉，由之行詐也！無臣而爲有臣，吾誰欺？欺天乎？且予與其死於臣之手也，無寧死於二三子之手乎！且予縱不得大葬，予死於道路乎？」

 ''''''九之十三''''''
子貢曰：「有美玉於斯，韞櫝而藏諸？求善賈而沽諸？」子曰：「沽之哉！沽之哉！我待賈者也！」

 ''''''九之十四''''''
子欲居九夷。或曰：「陋，如之何？」子曰：「君子居之，何陋之有？」

 ''''''九之十五''''''
子曰：「吾自衞反魯，然後樂正，雅頌各得其所。」

 ''''''九之十六''''''
子曰：「出則事公卿，入則事父兄，喪事不敢不勉，不爲酒困，何有於我哉？」

 ''''''九之十七''''''
子在川上曰：「逝者如斯夫！不舍晝夜。」

 ''''''九之十八''''''
子曰：「吾未見好德如好色者也。」

 ''''''九之十九''''''
子曰：「譬如爲山，未成一簣，止，吾止也！譬如平地，雖覆一簣，進，吾往也！」

 ''''''九之二十''''''
子曰：「語之而不惰者，其回也與！」

 ''''''九之二一''''''
子謂顏淵，曰：「惜乎！吾見其進也，未見其止也！」

 ''''''九之二二''''''
子曰：「苗而不秀者，有矣夫！秀而不實者，有矣夫！」

 ''''''九之二三''''''
子曰：「後生可畏，焉知來者之不如今也？四十、五十而無聞焉，斯亦不足畏也已！」

 ''''''九之二四''''''
子曰：「法語之言，能無從乎？改之爲貴。巽與之言，能無說乎？繹之爲貴。說而不繹，從而不改，吾末如之何也已矣！」

 ''''''九之二五''''''
子曰：「主忠信，毋友不如己者，過則勿憚改。」

 ''''''九之二六''''''
子曰：「三軍可奪帥也，匹夫不可奪志也。」

 ''''''九之二七''''''
子曰：「衣敝縕袍，與衣狐貉者立，而不恥者，其由也與！『不忮不求，何用不臧？』子路終身誦之。子曰：「是道也，何足以臧？」

 ''''''九之二八''''''
子曰：「歲寒，然後知松柏之後凋也。」

 ''''''九之二九''''''
子曰：「知者不惑，仁者不憂，勇者不懼。」

 ''''''九之三十''''''
子曰：「可與共學，未可與適道；可與適道，未可與立；可與立，未可與權。」

 ''''''九之三一''''''
「唐棣之華，偏其反而；豈不爾思？室是遠而」。子曰：「未之思也，夫何遠之有？」',1510);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),10,'鄉黨第十','*註疏
  ''''''十之一''''''
孔子於鄉黨，恂恂如也，似不能言者。其在宗廟朝廷，便便言，唯謹爾。

 ''''''十之二''''''
朝與下大夫言，侃侃如也；與上大夫言，誾誾如也。君在，踧踖如也，與與如也。

 ''''''十之三''''''
君召使擯，色勃如也，足躩如也。揖所與立，左右手，衣前後，襜如也。趨進，翼如也。賓退，必復命，曰：「賓不顧矣。」

 ''''''十之四''''''
入公門，鞠躬如也，如不容。立不中門，行不履閾。過位，色勃如也，足躩如也，其言似不足者。攝齊升堂，鞠躬如也，屛氣似不息者。出，降一等，逞顏色，怡怡如也。沒階趨進，翼如也。復其位，踧踖如也。

 ''''''十之五''''''
執圭，鞠躬如也，如不勝。上如揖，下如授，勃如戰色，足蹜蹜如有循。享禮，有容色。私覿，愉愉如也。

 ''''''十之六''''''
君子不以紺緅飾，紅紫不以爲褻服；當暑，袗絺綌，必表而出之。緇衣羔裘，素衣麑裘，黃衣狐裘。褻裘長，短右袂。（必有寢衣，長一身有半。）狐貉之厚以居。去喪，無所不佩。非帷裳，必殺之。羔裘玄冠，不以弔。吉月，必朝服而朝。

 ''''''十之七''''''
齊，必有明衣，布。齊必變食，居必遷坐。

 ''''''十之八''''''
食不厭精，膾不厭細。食饐而餲，魚餒而肉敗，不食。色惡不食，臭惡不食。失飪不食，不時不食。割不正不食，不得其醬不食。肉雖多，不使勝食氣。唯酒無量，不及亂。沽酒市脯不食。不撤薑食，不多食。祭于公，不宿肉。祭肉不出三日，出三日，不食之矣。食不語，寢不言。雖疏食菜羹瓜祭，必齊如也。

 ''''''十之九''''''
席不正不坐。

 ''''''十之十''''''
鄉人飮酒，杖者出，斯出矣。鄉人儺，朝服而立於阼階。

 ''''''十之十一''''''
問人於他邦，再拜而送之。康子饋藥，拜而受之，曰：「丘未達，不敢嘗。」

 ''''''十之十二''''''
廄焚，子退朝，曰：「傷人乎？」不問馬。

 ''''''十之十三''''''
君賜食，必正席先嘗之。君賜腥，必熟而薦之。君賜生，必畜之。侍食於君，君祭，先飯。疾，君視之，東首，加朝服拖紳。君命召，不俟駕行矣。

 ''''''十之十四''''''
入太廟，每事問。

 ''''''十之十五''''''
朋友死，無所歸，曰：「於我殯。」朋友之饋，雖車馬，非祭肉，不拜。

 ''''''十之十六''''''
寢不尸，居不容。見齊衰者，雖狎必變。見冕者與瞽者，雖褻必以貌。凶服者式之。式負版者。有盛饌，必變色而作。迅雷，風烈，必變。

 ''''''十之十七''''''
升車，必正立，執綏。車中不內顧，不疾言，不親指。

 ''''''十之十八''''''
色斯擧矣，翔而後集。曰：「山梁雌雉，時哉時哉！」子路共之，三嗅而作。',1078);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),11,'先進第十一','*註疏

 ''''''十一之一''''''
子曰：「先進於禮樂，野人也；後進於禮樂，君子也。如用之，則吾從先進。」

 ''''''十一之二''''''
子曰：「從我於陳蔡者，皆不及門也。」德行：顏淵、閔子騫、冉伯牛、仲弓。言語：宰我、子貢。政事：冉有、季路。文學：子游、子夏。

 ''''''十一之三''''''
子曰：「回也，非助我者也！於吾言，無所不說。」

 ''''''十一之四''''''
子曰：「孝哉，閔子騫！人不間於其父母昆弟之言。」

 ''''''十一之五''''''
南容三復《白圭》，孔子以其兄之子妻之。

 ''''''十一之六''''''
季康子問：「弟子孰爲好學？」孔子對曰：「有顏回者好學，不幸短命死矣。今也則亡。」

 ''''''十一之七''''''
顏淵死，顏路請子之車以爲之槨。子曰：「才不才，亦各言其子也。鯉也死，有棺而無槨。吾不徒行以爲之槨，以吾從大夫之後，不可徒行也。」

 ''''''十一之八''''''
顏淵死，子曰：「噫！天喪予！天喪予！」

 ''''''十一之九''''''
顏淵死，子哭之慟。從者曰：「子慟矣！」曰：「有慟乎？非夫人之爲慟而誰爲？」

 ''''''十一之十''''''
顏淵死，門人欲厚葬之。子曰：「不可。」門人厚葬之。子曰：「回也，視予猶父也，予不得視猶子也；非我也，夫二三子也。」

 ''''''十一之十一''''''
季路問事鬼神。子曰：「未能事人，焉能事鬼？」「敢問死？」曰：「未知生，焉知死？」

 ''''''十一之十二''''''
閔子侍側，誾誾如也；子路，行行如也；冉有、子貢，侃侃如也。子樂。「若由也，不得其死然！」

 ''''''十一之十三''''''
魯人爲長府。閔子騫曰：「仍舊貫，如之何？何必改作？」子曰：「夫人不言，言必有中。」

 ''''''十一之十四''''''
子曰：「由之瑟，奚爲於丘之門？」門人不敬子路。子曰：「由也升堂矣，未入於室也！」

 ''''''十一之十五''''''
子貢問：「師與商也孰賢？」子曰：「師也過，商也不及。」曰：「然則師愈與？」子曰：「過猶不及。」

 ''''''十一之十六''''''
季氏富於周公，而求也爲之聚斂而附益之。子曰：「非吾徒也！小子鳴鼓而攻之，可也。」

 ''''''十一之十七''''''
柴也愚，參也魯，師也辟，由也喭。

 ''''''十一之十八''''''
子曰：「回也其庶乎，屢空。賜不受命，而貨殖焉；億則屢中。」

 ''''''十一之十九''''''
子張問「善人」之道。子曰：「不踐跡，亦不入於室。」

 ''''''十一之二十''''''
子曰：「論篤是與，君子者乎？色莊者乎？」

 ''''''十一之二一''''''
子路問：「聞斯行諸？」子曰：「有父兄在，如之何其聞斯行之？」冉有問：「聞斯行諸？」子曰：「聞斯行之。」公西華曰：「由也問：『聞斯行諸？』子曰：『有父兄在。』求也問：『聞斯行諸？』子曰：『聞斯行之。』赤也惑，敢問。」子曰：「求也退，故進之；由也兼人，故退之。」

 ''''''十一之二二''''''
子畏於匡，顏淵後。子曰：「吾以女爲死矣！」曰：「子在，回何敢死？」

 ''''''十一之二三''''''
季子然問：「仲由、冉求可謂大臣與？」子曰：「吾以子爲異之問，曾由與求之問？所謂大臣者，以道事君，不可則止。今由與求也，可謂具臣矣。」曰：「然則從之者與？」子曰：「弒父與君，亦不從也。」

 ''''''十一之二四''''''
子路使子羔爲費宰。子曰：「賊夫人之子。」子路曰：「有民人焉，有社稷焉；何必讀書，然後爲學？」子曰：「是故惡夫佞者。」

 ''''''十一之二五''''''
子路、曾皙、冉有、公西華侍坐。子曰：「以吾一日長乎爾，毋吾以也。居則曰：『不吾知也。』如或知爾，則何以哉？」

子路率爾而對，曰：「千乘之國，攝乎大國之zh:閒;zh-hans:间;zh-hant:閒，加之以師旅，因之以饑饉，由也爲之，比及三年，可使有勇，且知方也。」夫子哂之。

「求，爾何如？」對曰：「方六七十，如五六十，求也爲之，比及三年，可使足民；如其禮樂，以俟君子。」

「赤，爾何如？」對曰：「非曰能之，願學焉！宗廟之事，如會同，端章甫，願爲小相焉。」

「點，爾何如？」鼓瑟希，鏗爾，舍瑟而作；對曰：「異乎三子者之撰。」子曰：「何傷乎？亦各言其志也。」曰：「莫春者，春服既成。冠者五六人，童子六七人。浴乎沂，風乎舞雩，詠而歸。」夫子喟然歎曰：「吾與點也！」

三子者出，曾皙後。曾皙曰：「夫三子者之言何如？」子曰：「亦各言其志也已矣。」曰：「夫子何哂由也？」曰：「爲國以禮，其言不讓，是故哂之。」「唯求則非邦也與？」「安見方六七十，如五六十，而非邦也者？」「唯赤則非邦也與？」「宗廟會同，非諸侯而何？赤也爲之小，孰能爲之大？」',1872);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),12,'顏淵第十二','*註疏

 ''''''十二之一''''''
顏淵問「仁」。子曰：「克己復禮爲仁。一日克己復禮，天下歸仁焉，爲仁由己，而由人乎哉？」顏淵曰：「請問其目。」子曰：「非禮勿視，非禮勿聽，非禮勿言，非禮勿動。」顏淵曰：「回雖不敏，請事斯語矣！」

 ''''''十二之二''''''
仲弓問「仁」。子曰：「出門如見大賓，使民如承大祭，己所不欲，勿施於人。在邦無怨，在家無怨。」仲弓曰：「雍雖不敏，請事斯語矣！」

 ''''''十二之三''''''
司馬牛問「仁」。子曰：「仁者，其言也訒。」曰：「其言也訒，斯謂之『仁』已夫？」子曰：「爲之難，言之得無訒乎！」

 ''''''十二之四''''''
司馬牛問「君子」。子曰：「君子不憂不懼。」曰：「不憂不懼，斯謂之『君子』矣夫？」子曰：「內省不疚，夫何憂何懼！」

 ''''''十二之五''''''
司馬牛憂曰：「人皆有兄弟，我獨亡！」子夏曰：「商聞之矣：『死生有命，富貴在天』君子敬而無失，與人恭而有禮，四海之內，皆兄弟也，君子何患乎無兄弟也！」

 ''''''十二之六''''''
子張問「明」。子曰：「浸潤之譖，膚受之愬，不行焉，可謂明也已矣。浸潤之譖，膚受之愬，不行焉，可謂遠也已矣。」

 ''''''十二之七''''''
子貢問「政」。子曰：「足食，足兵，民信之矣。」子貢曰：「必不得已而去，於斯三者何先？」曰：「去兵。」子貢曰：「必不得已而去，於斯二者何先？」曰：「去食。自古皆有死，民無信不立。」

 ''''''十二之八''''''
棘子成曰：「君子質而已矣，何以文爲？」子貢曰：「惜乎，夫子之說，君子也，駟不及舌！文猶質也，質猶文也；虎豹之鞹，猶犬羊之鞹。」

 ''''''十二之九''''''
哀公問於有若曰：「年饑，用不足，如之何？」有若對曰：「盍徹乎？」曰：「二，吾猶不足，如之何其徹也？」對曰：「百姓足，君孰與不足？百姓不足，君孰與足！」

 ''''''十二之十''''''
子張問「崇德，辨惑。」子曰：「主忠信，徙義：崇德也。愛之欲其生，惡之欲其死；既欲其生，又欲其死：是惑也。」（誠不以富，亦祇以異。）

 ''''''十二之十一''''''
齊景公問「政」於孔子。孔子對曰：「君君，臣臣，父父，子子。」公曰：「善哉！信如君不君，臣不臣，父不父，子不子，雖有粟，吾得而食諸？」

 ''''''十二之十二''''''
子曰：「片言可以折獄者，其由也與！」子路無宿諾。

 ''''''十二之十三''''''
子曰：「聽訟，吾猶人也；必也使無訟乎！」

 ''''''十二之十四''''''
子張問「政」。子曰：「居之無倦，行之以忠。」

 ''''''十二之十五''''''
子曰：「博學以文，約之以禮；亦可以弗畔矣夫！」

 ''''''十二之十六''''''
子曰：「君子成人之美，不成人之惡。小人反是。」

 ''''''十二之十七''''''
季康子問「政」於孔子。孔子對曰：「『政』者，正也。子帥以正，孰敢不正？」

 ''''''十二之十八''''''
季康子患盜，問於孔子。孔子對曰：「苟子之不欲，雖賞之不竊。」

 ''''''十二之十九''''''
季康子問政於孔子曰：「如殺無道，以就有道，何如？」孔子對曰：「子爲政，焉用殺？子欲善，而民善矣。君子之德，風；小人之德，草；草上之風，必偃。」

 ''''''十二之二十''''''
子張問：「士何如斯可謂之『達』矣？」子曰：「何哉，爾所謂『達』者？」子張對曰：「在邦必聞，在家必聞。」子曰：「是『聞』也，非『達』也。夫『達』也者，質直而好義，察言而觀色，慮以下人，在邦必達，在家必達。夫『聞』也者：色取仁而行違，居之不疑。在邦必聞，在家必聞。」

 ''''''十二之二一''''''
樊遲從遊於舞雩之下曰：「敢問崇德，脩慝，辨惑？」子曰：「善哉問！先事後得，非『崇德』與？攻其惡，無攻人之惡，非『脩慝』與？一朝之忿，忘其身以及其親，非『惑』與？」

 ''''''十二之二二''''''
樊遲問「仁」。子曰：「愛人。」問「知」。子曰：「知人。」樊遲未達，子曰：「擧直錯諸枉，能使枉者直。」樊遲退，見子夏曰：「鄉也吾見於夫子而問『知』，子曰：『擧直錯諸枉，能使枉者直』，何謂也？」子夏曰：「富哉言乎！舜有天下，選於眾，擧皋陶，不仁者遠矣。湯有天下，選於眾，擧伊尹，不仁者遠矣。」

 ''''''十二之二三''''''
子貢問「友」。子曰：「忠吿而善道之，不可則止，毋自辱焉。」

 ''''''十二之二四''''''
曾子曰：「君子以文會友，以友輔仁。」

Category:香港中學文憑考試指定文言篇章',1786);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),13,'子路第十三','*註疏
 ''''''十三之一''''''
子路問政。子曰：「先之，勞之。」請益，曰：「無倦。」

 ''''''十三之二''''''
仲弓爲季氏宰，問政。子曰：「先有司。赦小過。擧賢才。」曰：「焉知賢才而擧之？」曰：「擧爾所知。爾所不知，人其舍諸？」

 ''''''十三之三''''''
子路曰：「衞君待子而爲政，子將奚先？」子曰：「必也正名乎！」子路曰：「有是哉？子之迂也！奚其正？」子曰：「野哉，由也！君子於其所不知，蓋闕如也。名不正，則言不順；言不順，則事不成；事不成，則禮樂不興；禮樂不興，則刑罰不中；刑罰不中，則民無所措手足。故君子名之必可言也，言之必可行也。君子於其言，無所苟而已矣。」

 ''''''十三之四''''''
樊遲請學稼，子曰：「吾不如老農。」請學爲圃，曰：「吾不如老圃。」樊遲出，子曰：「小人哉，樊須也！上好禮，則民莫敢不敬；上好義，則民莫敢不服；上好信，則民莫敢不用情，夫如是，則四方之民，襁負其子而至矣，焉用稼？」

 ''''''十三之五''''''
子曰：「誦詩三百，授之以政，不達。使於四方，不能專對；雖多，亦奚以爲？」

 ''''''十三之六''''''
子曰：「其身正，不令而行；其身不正，雖令不從。」

 ''''''十三之七''''''
子曰：「魯衞之政，兄弟也。」

 ''''''十三之八''''''
子謂衞公子荊，「善居室：始有，曰：『苟合矣；』少有，曰：『苟完矣。』富有，曰：『苟美矣。』」

 ''''''十三之九''''''
子適衞，冉有僕。子曰：「庶矣哉！」冉有曰：「既庶矣，又何加焉？」曰：「富之。」曰：「既富矣，又何加焉？」曰：「教之。」

 ''''''十三之十''''''
子曰：「苟有用我者，期月而已可也，三年有成。」

 ''''''十三之十一''''''
子曰：「『善人爲邦百年，亦可以勝殘去殺矣。』誠哉是言也。」

 ''''''十三之十二''''''
子曰：「如有王者，必世而後仁。」

 ''''''十三之十三''''''
子曰：「苟正其身矣，於從政乎何有？不能正其身，如正人何？」

 ''''''十三之十四''''''
冉子退朝，子曰：「何晏也？」對曰：「有政。」子曰：「其事也！如有政，雖不吾以，吾其與聞之！」

 ''''''十三之十五''''''
定公問：「一言而可以興邦，有諸？」孔子對曰：「言不可以若是其幾也！人之言曰：『爲君難，爲臣不易』。如知爲君之難也，不幾乎一言而興邦乎？」曰：「一言而喪邦，有諸？」孔子對曰：「言不可以若是其幾也！人之言曰：『予無樂乎爲君，唯其言而莫予違也。』如其善而莫之違也，不亦善乎？如不善而莫之違也，不幾乎一言而喪邦乎？」

 ''''''十三之十六''''''
葉公問政。子曰：「近者說，遠者來。」

 ''''''十三之十七''''''
子夏爲莒父宰，問政。子曰：「無欲速，無見小利。欲速，則不達；見小利，則大事不成。」

 ''''''十三之十八''''''
葉公語孔子曰：「吾黨有直躬者，其父攘羊，而子證之。」孔子曰：「吾黨之直者異於是，父爲子隱，子爲父隱，直在其中矣。」

 ''''''十三之十九''''''
樊遲問仁。子曰：「居處恭，執事敬，與人忠。雖之夷狄，不可棄也。」

 ''''''十三之二十''''''
子貢問曰：「何如斯可謂之『士』矣？」子曰：「行己有恥，使於四方，不辱君命；可謂『士』矣。」曰：「敢問其次。」曰：「宗族稱孝焉，鄉黨稱弟焉。」曰：「敢問其次。」曰：「言必信，行必果，硜硜然，小人哉，抑亦可以爲次矣。」曰：「今之從政者何如？」子曰：「噫！斗筲之人，何足算也！」

 ''''''十三之二一''''''
子曰：「不得中行而與之，必也狂狷乎：狂者進取，狷者有所不爲也。」

 ''''''十三之二二''''''
子曰：「南人有言曰：『人而無恆，不可以作巫醫』。「善夫！『不恆其德，或承之羞』」子曰：「不占而已矣。」

 ''''''十三之二三''''''
子曰：「君子和而不同，小人同而不和。」

 ''''''十三之二四''''''
子貢問曰：「鄉人皆好之，何如？」子曰：「未可也。」「鄉人皆惡之，何如？」子曰：「未可也。不如鄉人之善者好之，其不善者惡之。」

 ''''''十三之二五''''''
子曰：「君子易事而難說也；說之不以道，不說也；及其使人也，器之。小人難事而易說也。說之雖不以道，說也；及其使人也，求備焉。」

 ''''''十三之二六''''''
子曰：「君子泰而不驕；小人驕而不泰。」

 ''''''十三之二七''''''
子曰：「剛毅木訥，近仁。」

 ''''''十三之二八''''''
子路問曰：「何如斯可謂之『士』矣？」子曰：「切切偲偲、怡怡如也，可謂『士』矣。朋友切切偲偲，兄弟怡怡。」

 ''''''十三之二九''''''
子曰：「善人教民七年，亦可以卽戎矣。」

 ''''''十三之三十''''''
子曰：「以不教民戰，是謂棄之。」',1891);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),14,'憲問第十四','*註疏

 ''''''十四之一''''''
憲問「恥」。子曰：「邦有道，穀；邦無道，穀，恥也。」

 ''''''十四之二''''''
「克、伐、怨、欲，不行焉，可以爲仁矣？」子曰：「可以爲難矣，仁則吾不知也。」

 ''''''十四之三''''''
子曰：「士而懷居，不足以爲士矣！」

 ''''''十四之四''''''
子曰：「邦有道，危言危行；邦無道，危行言孫。」

 ''''''十四之五''''''
子曰：「有德者必有言，有言者不必有德。仁者必有勇，勇者不必有仁。」

 ''''''十四之六''''''
南宮适問於孔子曰：「羿善射，奡盪舟，俱不得其死然。禹稷躬稼而有天下。」夫子不答。南宮适出，子曰：「君子哉若人！尙德哉若人！」

 ''''''十四之七''''''
子曰：「君子而不仁者有矣夫！未有小人而仁者也！」

 ''''''十四之八''''''
子曰：「愛之，能勿勞乎？忠焉，能勿誨乎？」

 ''''''十四之九''''''
子曰：「爲命，裨諶草創之，世叔討論之，行人子羽脩飾之，東里子產潤色之。」

 ''''''十四之十''''''
或問子產，子曰：「惠人也。」問子西。曰：「彼哉彼哉！」問管仲。曰：「人也，奪伯氏騈邑三百，飯疏食，沒齒，無怨言。」

 ''''''十四之十一''''''
子曰：「貧而無怨，難；富而無驕，易。」

 ''''''十四之十二''''''
子曰：「孟公綽，爲趙、魏老則優，不可以爲滕、薛大夫。」

 ''''''十四之十三''''''
子路問成人。子曰：「若臧武仲之知，公綽之不欲，卞莊子之勇，冉求之藝，文之以禮樂，亦可以爲成人矣！」曰：「今之成人者，何必然？見利思義，見危授命，久要不忘平生之言，亦可以爲成人矣！」

 ''''''十四之十四''''''
子問公叔文子於公明賈，曰：「信乎？夫子不言不笑不取乎？」公明賈對曰：「以吿者過也！夫子時然後言，人不厭其言；樂然後笑，人不厭其笑；義然後取，人不厭其取。」子曰：「其然！豈其然乎？」

 ''''''十四之十五''''''
子曰：「臧武仲以防，求爲後於魯，雖曰不要君，吾不信也。」

 ''''''十四之十六''''''
子曰：「晉文公譎而不正，齊桓公正而不譎。」

 ''''''十四之十七''''''
子路曰：「桓公殺公子糾，召忽死之，管仲不死。」曰：「未仁乎？」子曰：「桓公九合諸侯，不以兵車，管仲之力也。如其仁！如其仁！」

 ''''''十四之十八''''''
子貢曰：「管仲非仁者與？桓公殺公子糾，不能死，又相之。」子曰：「管仲相桓公，霸諸侯，一匡天下，民到于今受其賜；微管仲，吾其被髮左衽矣！豈若匹夫匹婦之爲諒也，自經於溝瀆，而莫之知也！」

 ''''''十四之十九''''''
公叔文子之臣大夫僎，與文子同升諸公。子聞之曰：「可以爲文矣！」

 ''''''十四之二十''''''
子言衞靈公之無道也。康子曰：「夫如是，奚而不喪？」孔子曰：「仲叔圉治賓客，祝鮀治宗廟，王孫賈治軍旅。夫如是，奚其喪？」

 ''''''十四之二一''''''
子曰：「其言之不怍，則爲之也難！」

 ''''''十四之二二''''''
陳成子弒簡公。孔子沐浴而朝，吿於哀公曰：「陳恆弒其君，請討之。」公曰：「吿夫三子。」孔子曰：「以吾從大夫之後，不敢不吿也！君曰：『吿夫三子』者！」之三子吿，不可。孔子曰：「以吾從大夫之後，不敢不吿也！」

 ''''''十四之二三''''''
子路問事君，子曰：「勿欺也，而犯之。」

 ''''''十四之二四''''''
子曰：「君子上達，小人下達。」

 ''''''十四之二五''''''
子曰：「古之學者爲己，今之學者爲人。」

 ''''''十四之二六''''''
蘧伯玉使人於孔子，孔子與之坐而問焉。曰：「夫子何爲？」對曰：「夫子欲寡其過而未能也。」使者出。子曰：「使乎！使乎！」

 ''''''十四之二七''''''
子曰：「不在其位，不謀其政。」

 ''''''十四之二八''''''
曾子曰：「君子思不出其位。」

 ''''''十四之二九''''''
子曰：「君子恥其言而過其行。」

 ''''''十四之三十''''''
子曰：「君子道者三，我無能焉：仁者不憂，知者不惑，勇者不懼。」子貢曰：「夫子自道也！」

 ''''''十四之三一''''''
子貢方人。子曰：「賜也，賢乎哉？夫我則不暇！」

 ''''''十四之三二''''''
子曰：「不患人之不己知，患其不能也。」

 ''''''十四之三三''''''
子曰：「不逆詐，不億不信，抑亦先覺者，是賢乎！」

 ''''''十四之三四''''''
微生畝謂孔子曰：「丘，何爲是栖栖者與？無乃爲佞乎？」孔子曰：「非敢爲佞也，疾固也。」

 ''''''十四之三五''''''
子曰：「驥不稱其力，稱其德也。」

 ''''''十四之三六''''''
或曰：「以德報怨，何如？」子曰：「何以報德？以直報怨，以德報德。」

 ''''''十四之三七''''''
子曰：「莫我知也夫！」子貢曰：「何爲其莫知子也？」子曰：「不怨天，不尤人，下學而上達，知我者，其天乎！」

 ''''''十四之三八''''''
公伯寮愬子路於季孫，子服景伯以吿，曰：「夫子固有惑志於公伯寮，吾力猶能肆諸市朝。」子曰：「道之將行也與，命也；道之將廢也與，命也。公伯寮其如命何？」

 ''''''十四之三九''''''
子曰：「賢者辟世，其次辟地，其次辟色，其次辟言。」

 ''''''十四之四十''''''
子曰：「作者七人矣。」

 ''''''十四之四一''''''
子路宿於石門。晨門曰：「奚自？」子路曰：「自孔氏。」曰：「是知其不可而爲之者與？」

 ''''''十四之四二''''''
子擊磬於衞。有荷蕢者而過孔氏之門者，曰：「有心哉！擊磬乎！」既而曰：「鄙哉，硜硜乎！莫己知也，斯已而已矣！『深則厲，淺則揭。』」子曰：「果哉！末之難矣！」

 ''''''十四之四三''''''
子張曰：「書云：『高宗諒陰，三年不言。』何謂也？」子曰：「何必高宗，古之人皆然。君薨，百官總己以聽於冢宰，三年。」

 ''''''十四之四四''''''
子曰：「上好禮，則民易使也。」

 ''''''十四之四五''''''
子路問君子。子曰：「脩己以敬。」曰：「如斯而已乎？」曰：「脩己以安人。」曰：「如斯而已乎？」曰：「脩己以安百姓。脩己以安百姓，堯舜其猶病諸！」

 ''''''十四之四六''''''
原壤夷俟。子曰：「幼而不孫弟，長而無述焉，老而不死，是爲賊。」以杖叩其脛。

 ''''''十四之四七''''''
闕黨童子將命。或問之曰：「益者與？」子曰：「吾見其居於位也，見其與先生並行也，非求益者也，欲速成者也。」

Category:香港中學文憑考試指定文言篇章',2563);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),15,'衞靈公第十五','*註疏

 ''''''十五之一''''''
衞靈公問陳於孔子。孔子對曰：「俎豆之事，則嘗聞之矣；軍旅之事，未之學也。」明日遂行。在陳絕糧。從者病，莫能興。子路慍見曰：「君子亦有窮乎？」子曰：「君子固窮，小人窮斯濫矣。」

 ''''''十五之二''''''
子曰：「賜也，女以予爲多學而識之者與？」對曰：「然，非與？」曰：「非也，予一以貫之。」

 ''''''十五之三''''''
子曰：「由，知德者鮮矣！」

 ''''''十五之四''''''
子曰：「無爲而治者，其舜也與！夫何爲哉？恭己正南面而已矣。」

 ''''''十五之五''''''
子張問行。子曰：「言忠信，行篤敬，雖蠻貊之邦行矣。言不忠信，行不篤敬，雖州里行乎哉？立，則見其參於前也；在輿，則見其倚於衡也。夫然後行。」子張書諸紳。

 ''''''十五之六''''''
子曰：「直哉史魚！邦有道，如矢；邦無道，如矢。君子哉蘧伯玉！邦有道，則仕；邦無道，則可卷而懷之。」

 ''''''十五之七''''''
子曰：「可與言，而不與之言，失人；不可與言，而與之言，失言。知者不失人，亦不失言。」

 ''''''十五之八''''''
子曰：「志士仁人，無求生以害仁，有殺身以成仁。」

 ''''''十五之九''''''
子貢問爲仁。子曰：「工欲善其事，必先利其器。居是邦也，事其大夫之賢者，友其士之仁者。」

 ''''''十五之十''''''
顏淵問爲邦。子曰：「行夏之時，乘殷之輅，服周之冕，樂則韶舞。放鄭聲，遠佞人。鄭聲淫，佞人殆。」

 ''''''十五之十一''''''
子曰：「人無遠慮，必有近憂。」

 ''''''十五之十二''''''
子曰：「已矣乎！吾未見好德如好色者也！」

 ''''''十五之十三''''''
子曰：「臧文仲，其竊位者與！知柳下惠之賢，而不與立也。」

 ''''''十五之十四''''''
子曰：「躬自厚，而薄責於人，則遠怨矣！」

 ''''''十五之十五''''''
子曰：「不曰『如之何，如之何』者，吾末如之何也已矣！」

 ''''''十五之十六''''''
子曰：「群居終日，言不及義，好行小慧，難矣哉！」

 ''''''十五之十七''''''
子曰：「君子義以爲質，禮以行之，孫以出之，信以成之。君子哉！」

 ''''''十五之十八''''''
子曰：「君子病無能焉，不病人之不己知也。」

 ''''''十五之十九''''''
子曰：「君子疾沒世而名不稱焉。」

 ''''''十五之二十''''''
子曰：「君子求諸己，小人求諸人。」

 ''''''十五之二一''''''
子曰：「君子矜而不爭，群而不黨。」

 ''''''十五之二二''''''
子曰：「君子不以言擧人，不以人廢言。」

 ''''''十五之二三''''''
子貢問曰：「有一言而可以終身行之者乎？」子曰：「其恕乎！己所不欲，勿施於人。」

 ''''''十五之二四''''''
子曰：「吾之於人也，誰毀誰譽？如有所譽者，其有所試矣。斯民也，三代之所以直道而行也。」

 ''''''十五之二五''''''
子曰：「吾猶及史之闕文也，有馬者，借人乘之。今亡矣夫！」

 ''''''十五之二六''''''
子曰：「巧言亂德，小不忍，則亂大謀。」

 ''''''十五之二七''''''
子曰：「眾惡之，必察焉；眾好之，必察焉。」

 ''''''十五之二八''''''
子曰：「人能弘道，非道弘人。」

 ''''''十五之二九''''''
子曰：「過而不改，是謂過矣。」

 ''''''十五之三十''''''
子曰：「吾嘗終日不食，終夜不寑，以思；無益，不如學也。」

 ''''''十五之三一''''''
子曰：「君子謀道不謀食。耕也，餒在其中矣；學也，祿在其中矣。君子憂道不憂貧。」

 ''''''十五之三二''''''
子曰：「知及之，仁不能守之，雖得之，必失之。知及之，仁能守之。不莊以蒞之，則民不敬。知及之，仁能守之，莊以蒞之。動之不以禮，未善也。」

 ''''''十五之三三''''''
子曰：「君子不可小知，而可大受也。小人不可大受，而可小知也。」

 ''''''十五之三四''''''
子曰：「民之於仁也，甚於水火。水火，吾見蹈而死者矣，未見蹈仁而死者也。」

 ''''''十五之三五''''''
子曰：「當仁，不讓於師。」

 ''''''十五之三六''''''
子曰：「君子貞而不諒。」

 ''''''十五之三七''''''
子曰：「事君，敬其事而後其食。」

 ''''''十五之三八''''''
子曰：「有教無類。」

 ''''''十五之三九''''''
子曰：「道不同，不相爲謀。」

 ''''''十五之四十''''''
子曰：「辭，達而已矣！」

 ''''''十五之四一''''''
師冕見。及階，子曰：「階也。」及席，子曰：「席也。」皆坐，子吿之曰：「某在斯，某在斯。」師冕出，子張問曰：「與師言之道與？」子曰：「然，固相師之道也。」

zh-hant;zh-hans|
 [http://www.ximalaya.com/swf/sound/red.swf?id=1411649 漢語普通話朗讀]
 （[http://www.ximalaya.com/4228109/sound/1411649 完整的外部鏈接]）
 Sound-icon.png|41px|
 此錄音来自外部鏈接、內容同2017年7月7日的版本、由 白雲出岫 使用漢語普通話錄製，不會隨條目修訂而自動改變。
 ''''''更多有聲文獻''''''
 Category:有聲文獻

Category:香港中學文憑考試指定文言篇章',2125);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),16,'季氏第十六','''''''十六之一''''''
季氏將伐顓臾。冉有季路見於孔子曰：「季氏將有事於顓臾。」孔子曰：「求！無乃爾是過與？夫顓臾，昔者先王以爲東蒙主，且在邦域之中矣，是社稷之臣也。何以伐爲？」冉有曰：「夫子欲之，吾二臣者，皆不欲也。」孔子曰：「求！周任有言曰：『陳力就列，不能者止。』危而不持，顚而不扶，則將焉用彼相矣？且爾言過矣！虎兕出於柙，龜玉毀於櫝中，是誰之過與？」冉有曰：「今夫顓臾，固而近於費；今不取，後世必爲子孫憂。」孔子曰：「求！君子疾夫舍曰欲之，而必爲之辭。丘也，聞有國有家者，不患寡而患不均，不患貧而患不安。蓋均無貧，和無寡，安無傾。夫如是，故遠人不服，則修文德以來之。既來之，則安之。今由與求也，相夫子，遠人不服而不能來也；邦分崩離析而不能守也，而謀動干戈於邦內。吾恐季孫之憂，不在顓臾，而在蕭牆之內也！」

 ''''''十六之二''''''
孔子曰：「天下有道，則禮樂征伐自天子出；天下無道，則禮樂征伐自諸侯出。自諸侯出，蓋十世希不失矣；自大夫出，五世希不失矣；陪臣執國命，三世希不失矣。天下有道，則政不在大夫。天下有道，則庶人不議。」

 ''''''十六之三''''''
孔子曰：「祿之去公室，五世矣。政逮於大夫，四世矣。故夫三桓之子孫，微矣。」

 ''''''十六之四''''''
孔子曰：「益者三友，損者三友：友直，友諒，友多聞，益矣；友便辟，友善柔，友便佞，損矣。」

 ''''''十六之五''''''
孔子曰：「益者三樂，損者三樂：樂節禮樂，樂道人之善，樂多賢友，益矣；樂驕樂，樂佚遊，樂宴樂，損矣。」

 ''''''十六之六''''''
孔子曰：「侍於君子有三愆：言未及之而言，謂之躁；言及之而不言，謂之隱；未見顏色而言，謂之瞽。」

 ''''''十六之七''''''
孔子曰：「君子有三戒：少之時，血氣未定，戒之在色；及其壯也，血氣方剛，戒之在鬭；及其老也，血氣既衰，戒之在得。」

 ''''''十六之八''''''
孔子曰：「君子有三畏：畏天命，畏大人，畏聖人之言。小人不知天命而不畏也，狎大人，侮聖人之言。」

 ''''''十六之九''''''
孔子曰：「生而知之者，上也；學而知之者，次也；困而學之，又其次也。困而不學，民斯爲下矣！」

 ''''''十六之十''''''
孔子曰：「君子有九思：視思明，聽思聰，色思溫，貌思恭，言思忠，事思敬，疑思問，忿思難，見得思義。」

 ''''''十六之十一''''''
孔子曰：「『見善如不及，見不善如探湯。』吾見其人矣，吾聞其語矣。『隱居以成其志，行義以達其道。』吾聞其語矣，未見其人也。」

 ''''''十六之十二''''''
（『誠不以富，亦祇以異。』）齊景公有馬千駟，死之日，民無德而稱焉。伯夷叔齊餓於首陽之下，民到于今稱之。其斯之謂與？

 ''''''十六之十三''''''
陳亢問於伯魚曰：「子亦有異聞乎？」對曰：「未也。嘗獨立，鯉趨而過庭。曰：『學《詩》乎？』對曰：『未也。』『不學《詩》，無以言。』鯉退而學《詩》。他日，又獨立，鯉趨而過庭。曰：『學禮乎？』對曰：『未也。』『不學禮，無以立！』鯉退而學禮。聞斯二者。」陳亢退而喜曰：「問一得三：聞《詩》，聞禮，又聞君子之遠其子也。」

 ''''''十六之十四''''''
邦君之妻，君稱之曰「夫人」，夫人自稱曰「小童」；邦人稱之曰「君夫人」，稱諸異邦曰「寡小君」；異邦人稱之，亦曰「君夫人」。',1346);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),17,'陽貨第十七','*註疏

 ''''''十七之一''''''
陽貨欲見孔子，孔子不見，歸孔子豚。孔子時其亡也，而往拜之，遇諸塗。謂孔子曰：「來，予與爾言。」曰：「懷其寶而迷其邦，可謂仁乎？」曰：「不可。」「好從事而亟失時，可謂知乎？」曰：「不可。」「日月逝矣，歲不我與。」孔子曰：「諾，吾將仕矣。」

 ''''''十七之二''''''
子曰：「性相近也，習相遠也。」

 ''''''十七之三''''''
子曰：「唯上知與下愚不移也。」

 ''''''十七之四''''''
子之武城，聞弦歌之聲，夫子莞爾而笑，曰：「割雞焉用牛刀。」子游對曰：「昔者，偃也聞諸夫子曰：『君子學道則愛人，小人學道則易使也。』」子曰：「二三子！偃之言是也，前言戲之耳！」

 ''''''十七之五''''''
公山弗擾以費畔，召，子欲往。子路不說，曰：「末之也已，何必公山氏之之也。」子曰：「夫召我者，而豈徒哉？如有用我者，吾其爲東周乎！」

 ''''''十七之六''''''
子張問「仁」於孔子。孔子曰：「能行五者於天下，爲仁矣。」「請問之？」曰：「恭、寬、信、敏、惠。恭則不侮，寬則得眾，信則人任焉，敏則有功，惠則足以使人。」

 ''''''十七之七''''''
佛肸召，子欲往。子路曰：「昔者由也聞諸夫子曰：『親於其身爲不善者，君子不入也。』佛肸以中牟畔，子之往也，如之何？」子曰：「然，有是言也。不曰堅乎？磨而不磷。不曰白乎？涅而不緇。吾豈匏瓜也哉？焉能繫而不食！」

 ''''''十七之八''''''
子曰：「由也，女聞『六言六蔽』矣乎？」對曰：「未也。」「居！吾語女。好仁不好學，其蔽也愚；好知不好學，其蔽也蕩；好信不好學，其蔽也賊；好直不好學，其蔽也絞；好勇不好學，其蔽也亂；好剛不好學，其蔽也狂。」

 ''''''十七之九''''''
子曰：「小子！何莫學夫《詩》？《詩》可以興，可以觀，可以群，可以怨；邇之事父，遠之事君；多識於鳥獸草木之名。」

 ''''''十七之十''''''
子謂伯魚曰：「女爲周南召南矣乎？人而不爲周南召南，其猶正牆面而立也與！」

 ''''''十七之十一''''''
子曰：「禮云禮云，玉帛云乎哉？樂云樂云！鐘鼓云乎哉？」

 ''''''十七之十二''''''
子曰：「色厲而內荏，譬諸小人，其猶穿窬之盜也與？」

 ''''''十七之十三''''''
子曰：「鄉原，德之賊也。」

 ''''''十七之十四''''''
子曰：「道聽而塗說，德之棄也。」

 ''''''十七之十五''''''
子曰：「鄙夫！可與事君也與哉？其未得之也，患得之；既得之，患失之。苟患失之，無所不至矣！」

 ''''''十七之十六''''''
子曰：「古者民有三疾，今也或是之亡也。古之狂也肆，今之狂也蕩；古之矜也廉，今之矜也忿戾；古之愚也直，今之愚也詐而已矣。」

 ''''''十七之十七''''''
子曰：「巧言令色，鮮矣仁。」

 ''''''十七之十八''''''
子曰：「惡紫之奪朱也，惡鄭聲之亂雅樂也，惡利口之覆邦家者。」

 ''''''十七之十九''''''
子曰：「予欲無言！」子貢曰：「子如不言，則小子何述焉？」子曰：「天何言哉？四時行焉，百物生焉，天何言哉？」

 ''''''十七之二十''''''
孺悲欲見孔子，孔子辭以疾。將命者出戶，取瑟而歌，使之聞之。

 ''''''十七之二一''''''
宰我問：「三年之喪，期已久矣！君子三年不爲禮，禮必壞；三年不爲樂，樂必崩。舊穀既沒，新穀既升，鑽燧改火，期可已矣。」子曰：「食夫稻，衣夫錦，於女安乎？」曰：「安！」「女安，則爲之！夫君子之居喪，食旨不甘，聞樂不樂，居處不安，故不爲也。今女安，則爲之！」宰我出。子曰：「予之不仁也！子生三年，然後免於父母之懷。夫三年之喪，天下之通喪也。予也，有三年之愛於其父母乎？」

 ''''''十七之二二''''''
子曰：「飽食終日，無所用心，難矣哉！不有博弈者乎？爲之，猶賢乎已！」

 ''''''十七之二三''''''
子路曰：「君子尙勇乎？」子曰：「君子義以爲上。君子有勇而無義爲亂，小人有勇而無義爲盜。」

 ''''''十七之二四''''''
子貢曰：「君子亦有惡乎？」子曰：「有惡，惡稱人之惡者，惡居下流而訕上者，惡勇而無禮者，惡果敢而窒者。」曰：「賜也亦有惡乎？」「惡徼以爲知者，惡不孫以爲勇者，惡訐以爲直者。」

 ''''''十七之二五''''''
子曰：「唯女子與小人爲難養也！近之則不孫，遠之則怨。」

 ''''''十七之二六''''''
子曰：「年四十而見惡焉，其終也已。」',1757);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),18,'微子第十八','*註疏

 ''''''十八之一''''''
微子去之，箕子爲之奴，比干諫而死。孔子曰：「殷有三仁焉。」

 ''''''十八之二''''''
柳下惠爲士師，三黜。人曰：「子未可以去乎？」曰：「直道而事人，焉往而不三黜？枉道而事人，何必去父母之邦？」

 ''''''十八之三''''''
齊景公待孔子，曰：「若季氏則吾不能，以季、孟之閒待之。」曰：「吾老矣，不能用也。」孔子行。

 ''''''十八之四''''''
齊人歸女樂，季桓子受之，三日不朝。孔子行。

 ''''''十八之五''''''
楚狂接輿，歌而過孔子，曰：「鳳兮！鳳兮！何德之衰？往者不可諫，來者猶可追。已而！已而！今之從政者殆而！」孔子下，欲與之言。趨而辟之，不得與之言。

 ''''''十八之六''''''
長沮、桀溺耦而耕。孔子過之，使子路問津焉。長沮曰：「夫執輿者爲誰？」子路曰：「爲孔丘。」曰：「是魯孔丘與？」曰：「是也。」曰：「是知津矣！」問於桀溺，桀溺曰：「子爲誰？」曰：「爲仲由。」曰：「是魯孔丘之徒與？」對曰：「然。」曰：「滔滔者，天下皆是也，而誰以易之？且而與其從辟人之士也，豈若從辟世之士哉？」耰而不輟。子路行以吿，夫子憮然曰：「鳥獸不可與同群！吾非斯人之徒與而誰與？天下有道，丘不與易也。」

 ''''''十八之七''''''
子路從而後，遇丈人，以杖荷蓧。子路問曰：「子見夫子乎？」丈人曰：「四體不勤，五穀不分，孰爲夫子？」植其杖而芸。子路拱而立。止子路宿，殺雞爲黍而食之，見其二子焉。明日，子路行以吿。子曰：「隱者也。」使子路反見之。至則行矣。子路曰：「不仕無義。長幼之節，不可廢也；君臣之義，如之何其廢之？欲潔其身，而亂大倫。君子之仕也，行其義也。道之不行，已知之矣。」

 ''''''十八之八''''''
逸民：伯夷、叔齊、虞仲、夷逸、朱張、柳下惠、少連。子曰：「不降其志，不辱其身，伯夷叔齊與！」謂柳下惠、少連：「降志辱身矣。言中倫，行中慮，其斯而已矣。」謂虞仲、夷逸：「隱居放言，身中淸，廢中權。」我則異於是，無可無不可。」

 ''''''十八之九''''''
大師摯適齊，亞飯干適楚，三飯繚適蔡，四飯缺適秦。鼓方叔入於河，播鼗武入於漢，少師陽、擊磬襄入於海。

 ''''''十八之十''''''
周公謂魯公曰：「君子不施其親，不使大臣怨乎不以。故舊無大故，則不棄也，無求備於一人。」

 ''''''十八之十一''''''
周有八士：伯達、伯适、仲突、仲忽、叔夜、叔夏、季隨、季騧。',985);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),19,'子張第十九','*註疏

 ''''''十九之一''''''
子張曰：「士見危致命，見得思義，祭思敬，喪思哀，其可已矣。」

 ''''''十九之二''''''
子張曰：「執德不弘，信道不篤，焉能爲有？焉能爲亡？」

 ''''''十九之三''''''
子夏之門人，問「交」於子張。子張曰：「子夏云何？」對曰：「子夏曰：『可者與之，其不可者拒之。』」子張曰：「異乎吾所聞；『君子尊賢而容眾，嘉善而矜不能。』我之大賢與，於人何所不容？我之不賢與，人將拒我，如之何其拒人也？」

 ''''''十九之四''''''
子夏曰：「雖小道，必有可觀者焉，致遠恐泥，是以君子不爲也。」

 ''''''十九之五''''''
子夏曰：「日知其所亡，月無忘其所能，可謂好學也已矣。」

 ''''''十九之六''''''
子夏曰：「博學而篤志，切問而近思，仁在其中矣。」

 ''''''十九之七''''''
子夏曰：「百工居肆以成其事，君子學以致其道。」

 ''''''十九之八''''''
子夏曰：「小人之過也必文。」

 ''''''十九之九''''''
子夏曰：「君子有三變：望之儼然，卽之也溫，聽其言也厲。」

 ''''''十九之十''''''
子夏曰：「君子信而後勞其民，未信則以爲厲己也。信而後諫，未信則以爲謗己也。」

 ''''''十九之十一''''''
子夏曰：「大德不踰閑，小德出入可也。」

 ''''''十九之十二''''''
子游曰：「子夏之門人小子，當洒掃應對進退則可矣，抑末也；本之則無，如之何？」子夏聞之曰：「噫！言游過矣！君子之道，孰先傳焉？孰後倦焉？譬諸草木，區以別矣。君子之道，焉可誣也？有始有卒者，其惟聖人乎！」

 ''''''十九之十三''''''
子夏曰：「仕而優則學，學而優則仕。」

 ''''''十九之十四''''''
子游曰：「喪致乎哀而止。」

 ''''''十九之十五''''''
子游曰：「吾友張也，爲難能也，然而未仁。」

 ''''''十九之十六''''''
曾子曰：「堂堂乎張也，難與並爲仁矣。」

 ''''''十九之十七''''''
曾子曰：「吾聞諸夫子：『人未有自致者也，必也親喪乎！』」

 ''''''十九之十八''''''
曾子曰：「吾聞諸夫子：『孟莊子之孝也，其他可能也，其不改父之臣，與父之政，是難能也。』」

 ''''''十九之十九''''''
孟氏使陽膚爲士師，問於曾子。曾子曰：「上失其道，民散久矣！如得其情，則哀矜而勿喜。」

 ''''''十九之二十''''''
子貢曰：「紂之不善，不如是之甚也。是以君子惡居下流，天下之惡皆歸焉。」

 ''''''十九之二一''''''
子貢曰：「君子之過也，如日月之食焉。過也，人皆見之；更也，人皆仰之。」

 ''''''十九之二二''''''
衞公孫朝問於子貢曰：「仲尼焉學？」子貢曰：「文、武之道，未墜於地，在人。賢者識其大者，不賢者識其小者，莫不有文、武之道焉。夫子焉不學？而亦何常師之有？」

 ''''''十九之二三''''''
叔孫武叔語大夫於朝，曰：「子貢賢於仲尼。」子服景伯以吿子貢。子貢曰：「譬之宮牆，賜之牆也及肩，闚見室家之好；夫子之牆數仞，不得其門而入，不見宗廟之美、百官之富。得其門者或寡矣。夫子之云，不亦宜乎！」

 ''''''十九之二四''''''
叔孫武叔毀仲尼。子貢曰：「無以爲也，仲尼不可毀也。他人之賢者，丘陵也，猶可踰也；仲尼，日月也，無得而踰焉。人雖欲自絕，其何傷於日月乎？多見其不知量也！」

 ''''''十九之二五''''''
陳子禽謂子貢曰：「子爲恭也，仲尼豈賢於子乎？」子貢曰：「君子一言以爲知，一言以爲不知，言不可不愼也！夫子之不可及也，猶天之不可階而升也。夫子之得邦家者，所謂『立之斯立，道之斯行，綏之斯來，動之斯和。其生也榮，其死也哀』。如之何其可及也？」',1461);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='論語' ORDER BY id DESC LIMIT 1),20,'堯曰第二十','*註疏

 ''''''二十之一''''''
堯曰：「咨！爾舜！天之曆數在爾躬，允執其中！四海困窮，天祿永終。」舜亦以命禹。曰：「予小子履，敢用玄牡，敢昭吿于皇皇后帝：有罪不敢赦。帝臣不蔽，簡在帝心！朕躬有罪，無以萬方；萬方有罪，罪在朕躬。」「周有大賚，善人是富。」「雖有周親，不如仁人；百姓有過，在予一人。」謹權量，審法度，脩廢官，四方之政行焉。興滅國，繼絕世，擧逸民，天下之民歸心焉。所重：民、食、喪、祭。寬則得眾，信則民任焉，敏則有功，公則說。

 ''''''二十之二''''''
子張問於孔子曰：「何如斯可以從政矣？」子曰：「尊五美，屛四惡，斯可以從政矣。」子張曰：「何謂五美？」子曰：「君子惠而不費，勞而不怨，欲而不貪，泰而不驕，威而不猛。」子張曰：「何謂惠而不費？」子曰：「因民之所利而利之，斯不亦惠而不費乎！擇可勞而勞之，又誰怨？欲仁而得仁，又焉貪？君子無眾寡，無小大，無敢慢，斯不亦泰而不驕乎？君子正其衣冠，尊其瞻視，儼然人望而畏之，斯不亦威而不猛乎！」子張曰：「何謂四惡？」子曰：「不教而殺謂之虐；不戒視成謂之暴；慢令致期謂之賊；猶之與人也，出納之吝，謂之貪。」

 ''''''二十之三''''''
子曰：「不知命，無以爲君子也；不知禮，無以立也；不知言，無以知人也。」',528);

INSERT INTO book (title,author,dynasty,category,category_sub,description,chapter_count) VALUES ('孫子兵法','孫武','春秋','兵家','武經七書','孫子兵法 ，春秋孫武所著，公版全文。','13');
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),1,'始計第一','孫子曰：兵者，國之大事，死生之地，存亡之道，不可不察也。故經之以五事，校之以七計，而索其情：一曰道，二曰天，三曰地，四曰將，五曰法。

道者，令民與上同意，可與之死，可與之生，而不畏危也。天者，陰陽、寒暑、時制也。地者，高下、遠近、險易、廣狹、死生也。將者，智、信、仁、勇、嚴也。法者，曲制、官道、主用也。凡此五者，將莫不聞，知之者勝，不知者不勝。故校之以七計而索其情，曰：主孰有道？將孰有能？天地孰得？法令孰行？兵眾孰強？士卒孰練？賞罰孰明？吾以此知勝負矣。

將聽吾計，用之必勝，留之；將不聽吾計，用之必敗，去之。計利以聽，乃為之勢，以佐其外。勢者，因利而制權也。兵者，詭道也。故能而示之不能，用而示之不用，近而示之遠，遠而示之近。利而誘之，亂而取之，實而備之，強而避之，怒而撓之，卑而驕之，佚而勞之，親而離之。攻其無備，出其不意。此兵家之勝，不可先傳也。

夫未戰而廟算勝者，得算多也；未戰而廟算不勝者，得算少也。多算勝，少算不勝，而況於無算乎？吾以此觀之，勝負見矣。',437);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),2,'作戰第二','孫子曰：凡用兵之法，馳車千駟，革車千乘，帶甲十萬，千里饋糧，則內外之費，賓客之用，膠漆之材，車甲之奉，日費千金，然後十萬之師舉矣。其用戰也，貴勝，久則鈍兵挫銳，攻城則力屈，久暴師則國用不足。夫鈍兵挫銳，屈力殫貨，則諸侯乘其弊而起，雖有智者，不能善其後矣。故兵聞拙速，未睹巧之久也。夫兵久而國利者，未之有也。故不盡知用兵之害者，則不能盡知用兵之利也。

善用兵者，役不再籍，糧不三載；取用於國，因糧於敵，故軍食可足也。

國之貧於師者遠輸，遠輸則百姓貧；近師者貴賣，貴賣則百姓財竭，財竭則急於丘役。力屈財殫，中原內虛於家。百姓之費，十去其七；公家之費，破車罷馬，甲胄矢弩，戟楯蔽櫓，丘牛大車，十去其六。

故智將務食於敵，食敵一鍾，當吾二十鍾；萁稈一石，當吾二十石。

故殺敵者，怒也；取敵之利者，貨也。故車戰，得車十乘以上，賞其先得者，而更其旌旗。車雜而乘之，卒善而養之，是謂勝敵而益強。

故兵貴勝，不貴久。故知兵之將，民之司命，國家安危之主也。',424);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),3,'謀攻第三','孫子曰：凡用兵之法，全國為上，破國次之；全軍為上，破軍次之；全旅為上，破旅次之；全卒為上，破卒次之；全伍為上，破伍次之。是故百戰百勝，非善之善者也；不戰而屈人之兵，善之善者也。

故上兵伐謀，其次伐交，其次伐兵，其下攻城。攻城之法，為不得已。修櫓轒轀，具器械，三月而後成；距闉，又三月而後已。將不勝其忿，而蟻附之，殺士三分之一，而城不拔者，此攻之災也。

故善用兵者，屈人之兵而非戰也，拔人之城而非攻也，毀人之國而非久也，必以全爭於天下，故兵不頓而利可全，此謀攻之法也。

故用兵之法，十則圍之，五則攻之，倍則分之，敵則能戰之，少則能守之，不若則能避之。故小敵之堅，大敵之擒也。

夫將者，國之輔也。輔周則國必強，輔隙則國必弱。

故君之所以患於軍者三：不知軍之不可以進而謂之進，不知軍之不可以退而謂之退，是為縻軍；不知三軍之事，而同三軍之政，則軍士惑矣；不知三軍之權，而同三軍之任，則軍士疑矣。三軍既惑且疑，則諸侯之難至矣，是謂亂軍引勝。

故知勝有五：知可以戰與不可以戰者勝，識衆寡之用者勝，上下同欲者勝，以虞待不虞者勝，將能而君不御者勝。此五者，知勝之道也。

故曰：知彼知己，百戰不殆；不知彼而知己，一勝一負；不知彼不知己，每戰必殆。',521);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),4,'軍形第四','孫子曰：昔之善戰者，先為不可勝，以待敵之可勝。不可勝在己，可勝在敵。故善戰者，能為不可勝，不能使敵之必可勝。故曰：勝可知，而不可為。

不可勝者，守也；可勝者，攻也。守則不足，攻則有餘。善守者，藏於九地之下；善攻者，動於九天之上，故能自保而全勝也。

見勝不過衆人之所知，非善之善者也；戰勝而天下曰善，非善之善者也。故舉秋毫不為多力，見日月不為明目，聞雷霆不為聰耳。

古之所謂善戰者，勝於易勝者也。故善戰者之勝也，無智名，無勇功。故其戰勝不忒。不忒者，其所措必勝，勝已敗者也。故善戰者，立於不敗之地，而不失敵之敗也。是故勝兵先勝而後求戰，敗兵先戰而後求勝。善用兵者，修道而保法，故能為勝敗之政。

兵法：一曰度，二曰量，三曰數，四曰稱，五曰勝。地生度，度生量，量生數，數生稱，稱生勝。故勝兵若以鎰稱銖，敗兵若以銖稱鎰。勝者之戰民也，若決積水於千仞之溪者，形也。',381);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),5,'兵勢第五','孫子曰：凡治衆如治寡，分數是也；鬥衆如鬥寡，形名是也；三軍之衆，可使必受敵而無敗者，奇正是也；兵之所加，如以碫投卵者，虛實是也。

凡戰者，以正合，以奇勝。故善出奇者，無窮如天地，不竭如江海。終而復始，日月是也。死而復生，四時是也。聲不過五，五聲之變，不可勝聽也；色不過五，五色之變，不可勝觀也；味不過五，五味之變，不可勝嘗也；戰勢不過奇正，奇正之變，不可勝窮也。奇正相生，如循環之無端，孰能窮之哉？

激水之疾，至於漂石者，勢也；鷙鳥之疾，至於毀折者，節也。故善戰者，其勢險，其節短。勢如彍弩，節如發機。

紛紛紜紜，鬥亂而不可亂也；渾渾沌沌，形圓而不可敗也。亂生於治，怯生於勇，弱生於強。治亂，數也；勇怯，勢也；強弱，形也。

故善動敵者，形之，敵必從之；予之，敵必取之。以利動之，以卒待之。

故善戰者，求之於勢，不責於人，故能擇人而任勢。任勢者，其戰人也，如轉木石。木石之性，安則靜，危則動，方則止，圓則行。故善戰人之勢，如轉圓石於千仞之山者，勢也。',429);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),6,'虛實第六','孫子曰：凡先處戰地而待敵者佚，後處戰地而趨戰者勞。故善戰者，致人而不致於人。

能使敵自至者，利之也；能使敵不得至者，害之也。故敵佚能勞之，飽能饑之，安能動之。出其所不趨，趨其所不意。

行千里而不勞者，行於無人之地也。攻而必取者，攻其所不守也；守而必固者，守其所不攻也。故善攻者，敵不知其所守；善守者，敵不知其所攻。微乎微乎！至于無形；神乎神乎！至于無聲，故能為敵之司命。

進而不可禦者，沖其虛也；退而不可追者，速而不可及也。故我欲戰，敵雖高壘深溝，不得不與我戰者，攻其所必救也；我不欲戰，雖畫地而守之，敵不得與我戰者，乖其所之也。

故形人而我無形，則我專而敵分。我專為一，敵分為十，是以十攻其一也，則我衆而敵寡。能以衆擊寡者，則吾之所與戰者，約矣。吾所與戰之地不可知，不可知，則敵所備者多，敵所備者多，則吾之所與戰者寡矣。故備前則後寡，備後則前寡，備左則右寡，備右則左寡，無所不備，則無所不寡。寡者，備人者也；衆者，使人備己者也。

故知戰之地，知戰之日，則可千里而會戰；不知戰之地，不知戰之日，則左不能救右，右不能救左，前不能救後，後不能救前，而況遠者數十里，近者數里乎！以吾度之，越人之兵雖多，亦奚益於勝敗哉！故曰：勝可也。敵雖衆，可使無鬥。

故策之而知得失之計，作之而知動靜之理，形之而知死生之地，角之而知有餘不足之處。故形兵之極，至於無形。無形，則深間不能窺，智者不能謀。因形而措勝於衆，衆不能知。人皆知我所以勝之形，而莫知吾所以制勝之形。故其戰勝不復，而應形於無窮。

夫兵形象水，水之行，避高而趨下；兵之勝，避實而擊虛。水因地而制行，兵因敵而制勝。故兵無成勢，無恒形，能因敵變化而取勝者，謂之神。故五行無常勝，四時無常位，日有短長，月有死生。',732);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),7,'軍爭第七','孫子曰：凡用兵之法，將受命於君，合軍聚衆，交和而舍，莫難於軍爭。軍爭之難者，以迂為直，以患為利。故迂其途，而誘之以利，後人發，先人至，此知迂直之計者也。

故軍爭為利，軍爭為危。舉軍而爭利，則不及；委軍而爭利，則輜重捐。是故卷甲而趨，日夜不處，倍道兼行，百里而爭利，則擒三軍將，勁者先，疲者後，其法十一而至；五十里而爭利，則蹶上軍將，其法半至；三十里而爭利，則三分之二至。是故軍無輜重則亡，無糧食則亡，無委積則亡。故不知諸侯之謀者，不能豫交；不知山林、險阻、沮澤之形者，不能行軍；不用鄉導者，不能得地利。

故兵以詐立，以利動，以分合為變者也。故其疾如風，其徐如林，侵掠如火，不動如山，難知如陰，動如雷震。掠鄉分衆，廓地分利，懸權而動。先知迂直之計者勝，此軍爭之法也。

《軍政》曰：「言不相聞，故為金鼓；視不相見，故為旌旗。」夫金鼓旌旗者，所以一民之耳目也。民既專一，則勇者不得獨進，怯者不得獨退，此用衆之法也。故夜戰多金鼓，晝戰多旌旗，所以變人之耳目也。

三軍可奪氣，將軍可奪心。是故朝氣銳，晝氣惰，暮氣歸。故善用兵者，避其銳氣，擊其惰歸，此治氣者也；以治待亂，以靜待嘩，此治心者也；以近待遠，以佚待勞，以飽待饑，此治力者也；無邀正正之旗，無擊堂堂之陣，此治變者也。

故用兵之法，高陵勿向，背丘勿逆，佯北勿從，銳卒勿攻，餌兵勿食，歸師勿遏，圍師必闕，窮寇勿迫，此用兵之法也。',591);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),8,'九變第八','孫子曰：凡用兵之法，將受命於君，合軍聚眾。圮地無舍，衢地合交，絕地無留，圍地則謀，死地則戰。途有所不由，軍有所不擊，城有所不攻，地有所不爭，君命有所不受。故將通於九變之利者，知用兵矣；將不通於九變之利，雖知地形，不能得地之利矣；治兵不知九變之術，雖知地利，不能得人之用矣。

是故智者之慮，必雜於利害，雜於利而務可信也，雜於害而患可解也。是故屈諸侯者以害，役諸侯者以業，趨諸侯者以利。

故用兵之法，無恃其不來，恃吾有以待之；無恃其不攻，恃吾有所不可攻也。

故將有五危︰必死，可殺也﹔必生，可虜也﹔忿速，可侮也﹔廉潔，可辱也﹔愛民，可煩也。凡此五者，將之過也，用兵之災也。覆軍殺將，必以五危，不可不察也。',303);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),9,'行軍第九','孫子曰：凡處軍相敵，絕山依谷，視生處高，戰隆無登，此處山之軍也。絕水必遠水，客絕水而來，勿迎之于水內，令半濟而擊之，利；欲戰者，無附于水而迎客，視生處高，無迎水流，此處水上之軍也。絕斥澤，惟亟去無留，若交軍於斥澤之中，必依水草，而背衆樹，此處斥澤之軍也。平陸處易，而右背高，前死後生，此處平陸之軍也。凡此四軍之利，黃帝之所以勝四帝也。

凡軍好高而惡下，貴陽而賤陰，養生而處實，軍無百疾，是謂必勝。丘陵堤防，必處其陽，而右背之，此兵之利，地之助也。上雨，水沫至，欲涉者，待其定也。

凡地有絕澗，遇天井、天牢、天羅、天陷、天隙，必亟去之，勿近也。吾遠之，敵近之；吾迎之，敵背之。軍旁有險阻、潢井、葭葦、林木、蘙薈者，必謹覆索之，此伏奸之所處也。

敵近而靜者，恃其險也；遠而挑戰者，欲人之進也；其所居易者，利也；衆樹動者，來也；衆草多障者，疑也；鳥起者，伏也；獸駭者，覆也；塵高而銳者，車來也；卑而廣者，徒來也；散而條達者，樵采也；少而往來者，營軍也；辭卑而益備者，進也；辭強而進驅者，退也；輕車先出，居其側者，陣也；無約而請和者，謀也；奔走而陳兵者，期也；半進半退者，誘也；杖而立者，饑也；汲而先飲者，渴也；見利而不進者，勞也；鳥集者，虛也；夜呼者，恐也；軍擾者，將不重也；旌旗動者，亂也；吏怒者，倦也；粟馬肉食，軍無懸缻，而不返其舍者，窮寇也；諄諄翕翕，徐與人言者，失衆也；數賞者，窘也；數罰者，困也；先暴而後畏其衆者，不精之至也；來委謝者，欲休息也。兵怒而相迎，久而不合，又不相去，必謹察之。

故兵非貴，惟無武進，足以併力、料敵、取人而已。夫惟無慮而易敵者，必擒於人。

卒未親附而罰之，則不服，不服則難用也。卒已親附而罰不行，則不可用也。故令之以文，齊之以武，是謂必取。令素行以教其民，則民服；令素不行以教其民，則民不服。令素行者，與衆相得也。',777);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),10,'地形第十','孫子曰：凡地形有通者、有掛者、有支者、有隘者、有險者、有遠者。我可以往，彼可以來，曰通。通形者，先居高陽，利糧道，以戰則利。可以往，難以返，曰掛。掛形者，敵無備，出而勝之；敵若有備，出而不勝，難以返，不利。我出而不利，彼出而不利，曰支。支形者，敵雖利我，我無出也，引而去之，令敵半出而擊之，利。隘形者，我先居之，必盈之以待敵。若敵先居之，盈而勿從，不盈而從之。險形者，我先居之，必居高陽以待敵；若敵先居之，引而去之，勿從也。遠形者，勢均，難以挑戰，戰而不利。凡此六者，地之道也，將之至任，不可不察也。

故兵有走者、有弛者、有陷者、有崩者、有亂者、有北者。凡此六者，非天之災，將之過也。夫勢均，以一擊十，曰走；卒强吏弱，曰弛；吏强卒弱，曰陷；大吏怒而不服，遇敵懟而自戰，將不知其能，曰崩；將弱不嚴，教道不明，吏卒無常，陳兵縱橫，曰亂；將不能料敵，以少合衆，以弱擊強，兵無選鋒，曰北。凡此六者，敗之道也，將之至任，不可不察也。

夫地形者，兵之助也。料敵制勝，計險厄遠近，上將之道也。知此而用戰者必勝，不知此而用戰者必敗。故戰道必勝，主曰無戰，必戰可也；戰道不勝，主曰必戰，無戰可也。是故進不求名，退不避罪，唯民是保，而利合於主，國之寶也。

視卒如嬰兒，故可以與之赴深溪；視卒如愛子，故可與之俱死。厚而不能使，愛而不能令，亂而不能治，譬若驕子，不可用也。

知吾卒之可以擊，而不知敵之不可擊，勝之半也；知敵之可擊，而不知吾卒之不可以擊，勝之半也；知敵之可擊，知吾卒之可以擊，而不知地形之不可以戰，勝之半也。故知兵者，動而不迷，舉而不窮。故曰：知彼知己，勝乃不殆；知天知地，勝乃可全。',691);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),11,'九地第十一','孫子曰：凡用兵之法，有散地，有輕地，有爭地，有交地，有衢地，有重地，有圮地，有圍地，有死地。諸侯自戰其地者，為散地；入人之地而不深者，為輕地；我得則利，彼得亦利者，為爭地；我可以往，彼可以來者，為交地；諸侯之地三屬，先至而得天下之衆者，為衢地；入人之地深，背城邑多者，為重地；山林、險阻、沮澤，凡難行之道者，為圮地；所由入者隘，所從歸者迂，彼寡可以擊吾之衆者，為圍地；疾戰則存，不疾戰則亡者，為死地。是故散地則無戰，輕地則無止，爭地則無攻，交地則無絕，衢地則合交，重地則掠，圮地則行，圍地則謀，死地則戰。

所謂古之善用兵者，能使敵人前後不相及，衆寡不相恃，貴賤不相救，上下不相收，卒離而不集，兵合而不齊。合於利而動，不合於利而止。敢問︰「敵衆整而將來，待之若何？」曰：「先奪其所愛，則聽矣。」故兵之情主速，乘人之不及，由不虞之道，攻其所不戒也。

凡為客之道，深入則專，主人不克。掠于饒野，三軍足食。謹養而勿勞，併氣積力，運兵計謀，為不可測。投之無所往，死且不北。死焉不得，士人盡力。兵士甚陷則不懼，無所往則固，深入則拘，不得已則鬥。是故其兵不修而戒，不求而得，不約而親，不令而信。禁祥去疑，至死無所之。吾士無餘財，非惡貨也；無餘命，非惡壽也。令發之日，士卒坐者涕沾襟，偃臥者淚交頤。投之無所往者，則諸、劌之勇也。

故善用兵者，譬如率然。率然者，常山之蛇也。擊其首則尾至，擊其尾則首至，擊其中則首尾俱至。敢問︰「兵可使如率然乎？」曰︰「可。夫吳人與越人相惡也，當其同舟而濟。遇風，其相救也，如左右手。」是故方馬埋輪，未足恃也；齊勇如一，政之道也；剛柔皆得，地之理也。故善用兵者，攜手若使一人，不得已也。

將軍之事，靜以幽，正以治。能愚士卒之耳目，使之無知；易其事，革其謀，使人無識；易其居，迂其途，使人不得慮。帥與之期，如登高而去其梯；帥與之深入諸侯之地，而發其機，焚舟破釜，若驅群羊。驅而往，驅而來，莫知所之。聚三軍之衆，投之於險，此謂將軍之事也。九地之變，屈伸之利，人情之理，不可不察也。

凡為客之道，深則專，淺則散。去國越境而師者，絕地也；四達者，衢地也；入深者，重地也；入淺者，輕地也；背固前隘者，圍地也；無所往者，死地也。是故散地，吾將一其志；輕地，吾將使之屬；爭地，吾將趨其後；交地，吾將謹其守；衢地，吾將固其結；重地，吾將繼其食；圮地，吾將進其途；圍地，吾將塞其闕；死地，吾將示之以不活。故兵之情：圍則禦，不得已則鬥，過則從。

是故不知諸侯之謀者，不能豫交；不知山林、險阻、沮澤之形者，不能行軍；不用鄉導者，不能得地利。四五者，不知一，非霸王之兵也。夫霸王之兵，伐大國，則其衆不得聚；威加於敵，則其交不得合。是故不爭天下之交，不養天下之權，信己之私，威加於敵，則其城可拔，其國可隳。施無法之賞，懸無政之令。犯三軍之衆，若使一人。犯之以事，勿告以言；犯之以利，勿告以害。投之亡地然後存，陷之死地然後生。夫衆陷於害，然後能為勝敗。故為兵之事，在於佯順敵之意，併敵一向，千里殺將，是謂巧能成事者也。

是故政舉之日，夷關折符，無通其使；厲於廊廟之上，以誅其事。敵人開闔，必亟入之，先其所愛，微與之期，踐墨隨敵，以決戰事。是故始如處女，敵人開戶；後如脫兔，敵不及拒。',1341);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),12,'火攻第十二','孫子曰：凡火攻有五：一曰火人，二曰火積，三曰火輜，四曰火庫，五曰火隊。行火必有因，烟火必素具。發火有時，起火有日。時者，天之燥也。日者，月在箕、壁、翼、軫也。凡此四宿者，風起之日也。

凡火攻，必因五火之變而應之：火發於內，則早應之於外；火發而其兵靜者，待而勿攻，極其火力，可從而從之，不可從則止；火可發於外，無待於內，以時發之；火發上風，無攻下風；晝風久，夜風止。凡軍必知有五火之變，以數守之。故以火佐攻者明，以水佐攻者強。水可以絕，不可以奪。

夫戰勝攻取，而不修其功者凶，命曰「費留」。故曰：明主慮之，良將修之，非利不動，非得不用，非危不戰。主不可以怒而興師，將不可以慍而致戰。合於利而動，不合於利而止。怒可以復喜，慍可以復悅，亡國不可以復存，死者不可以復生。故明主慎之，良將警之，此安國全軍之道也。',353);
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='孫子兵法' ORDER BY id DESC LIMIT 1),13,'用間第十三','孫子曰：凡興師十萬，出征千里，百姓之費，公家之奉，日費千金，內外騷動，怠于道路，不得操事者，七十萬家。相守數年，以爭一日之勝，而愛爵祿百金，不知敵之情者，不仁之至也，非人之將也，非主之佐也，非勝之主也。故明君賢將，所以動而勝人，成功出於眾者，先知也。先知者，不可取於鬼神，不可象於事，不可驗於度，必取於人，知敵之情者也。

故用間有五：有鄉間，有內間，有反間，有死間，有生間。五間俱起，莫知其道，是謂「神紀」，人君之寶也。鄉間者，因其鄉人而用之；內間者，因其官人而用之；反間者，因其敵間而用之；死間者，為誑事於外，令吾間知之，而傳於敵間也；生間者，反報也。

故三軍之事，莫親於間，賞莫厚於間，事莫密於間，非聖智不能用間，非仁義不能使間，非微妙不能得間之實。微哉！微哉！無所不用間也。間事未發而先聞者，間與所告者皆死。

凡軍之所欲擊，城之所欲攻，人之所欲殺，必先知其守將、左右、謁者、門者、舍人之姓名，令吾間必索知之。必索敵人之間來間我者，因而利之，導而舍之，故反間可得而用也；因是而知之，故鄉間、內間可得而使也；因是而知之，故死間為誑事，可使告敵；因是而知之，故生間可使如期。五間之事，主必知之，知之必在於反間，故反間不可不厚也。

昔殷之興也，伊摯在夏；周之興也，呂牙在殷。故明君賢將，能以上智為間者，必成大功。此兵之要，三軍之所恃而動也。',573);

INSERT INTO book (title,author,dynasty,category,category_sub,description,chapter_count) VALUES ('千字文','周興嗣','南朝梁','蒙學','開蒙要篇','千字文 ，南朝梁周興嗣所著，公版全文。','1');
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='千字文' ORDER BY id DESC LIMIT 1),1,'全文','天地黃　宇宙洪荒
日月盈昃　辰宿列張
寒來暑往　秋收冬藏
閏餘成歲　律調陽
雲騰致雨　露結爲霜
金生麗水　玉出崗
劍號巨闕　珠稱夜光
菓珎李柰　菜重芥薑
海河淡　鱗潛羽翔
龍師火帝　鳥官人皇
始文字　乃服衣裳
推位讓國　有虞陶唐
弔民伐罪　周發湯
朝問道　拱平章
愛育黎首　臣伏戎羌
遐迩體　率賓歸王
鳴鳳在　白駒食場
化被草木　賴及萬方
盖此身　四大五常
恭惟鞠養　豈敢毀傷
女慕貞　男效才良
知過必改　能莫忘
罔談短　靡恃己長
信使可覆　器欲難量
墨悲絲　詩讚羔羊
景行維賢　念作聖
建名立　形端表正
空谷傳聲　虛堂習聽
禍因惡　福緣善慶
尺璧非寶　寸陰是
資父事君　曰嚴敬
孝當竭力　忠則盡命
臨深履薄　夙興溫
似蘭斯馨　如松之盛
川流不息　淵澄取
容止若思　言辭安定
篤初美　慎終宜令
榮業所基　籍甚無竟
學優登仕　攝職從政
存以甘　去而益詠
樂殊貴賤　禮別尊卑
上和下睦　夫唱婦随
外受傅訓　入奉母儀
諸姑伯叔　猶子比兒
孔懷兄弟　同氣連枝
交友投分　切磨箴規
仁慈隱惻　造次離
節義廉退　顛沛匪虧
性靜情逸　心動神疲
守眞志滿　逐物意移
堅持雅　好爵自
都邑華夏　東西京
背面洛　浮渭據涇
宮殿盤鬱　樓觀飛驚
圖寫禽獸　畫仙靈
丙舍傍啟　甲帳楹
肆筵設席　鼓瑟吹笙
階納陛　弁轉疑星
右通廣內　左達承明
既集墳典　亦聚羣英
杜鍾隸　書經
府羅將相　路槐卿
戶封八縣　家給千兵
高冠陪輦　轂振纓
世祿侈富　車駕肥輕
策功茂實　勒碑刻銘
磻伊尹　佐時阿衡
奄宅曲阜　微旦孰營
桓公匡合　濟弱扶傾
綺廻漢惠　感武丁
俊密勿　多士寔寧
晉楚更霸　趙魏困橫
假途滅虢　踐土會盟
何遵約法　韓弊煩
起翦頗牧　用軍最精
宣威沙　馳譽丹青
九州禹跡　百郡秦
嶽宗岱　禪主云亭
雁門紫塞　田赤城
池碣石　鉅野洞庭
曠遠緜邈　巖岫杳冥
治本於農　務稼穡
俶載南畝　藝黍稷
稅熟貢新　勸黜陟
孟軻敦素　秉直
庶幾中庸　勞謙謹
聆音察理　鑑貌色
貽厥嘉猷　勉其祗植
省躬譏誡　寵增抗極
殆辱近　幸即
兩見機　解組誰逼
索居處　沈默寂
古尋論　逍遙
欣奏累遣　慼謝歡招
渠的歷　園莽抽條
晚翠　梧桐
陳根委翳　落葉飄
游鵾獨運　凌摩絳霄
耽讀市　寓目囊箱
易輶攸畏　屬耳垣
具膳飯　適口充腸
飽飫宰　飢厭糟糠
親戚故舊　老少異
妾御績紡　侍巾帷房
紈扇潔　銀燭煌
晝眠夕寐　籃象
歌酒讌　接杯舉觴
矯手頓足　悅豫且康
嫡後嗣續　祭祀嘗
稽顙再拜　悚懼恐惶
牒簡要　顧答審詳
骸垢想　執熱願涼
驢騾犢特　駭躍超驤
誅斬賊盜　捕獲叛亡
布射遼丸　嵇琴阮嘯
恬筆倫紙　鈞巧任釣
釋紛利俗　並皆佳妙
毛施淑姿　工笑
年矢每催　暉朗
璣懸斡　晦魄環照
指薪脩祜　永綏吉劭
矩步引領　俯仰廊廟
束帶矜莊　徘徊瞻眺
孤陋寡聞　愚蒙等誚
謂語助者　焉哉乎也

Category:啟蒙書籍',1178);

INSERT INTO book (title,author,dynasty,category,category_sub,description,chapter_count) VALUES ('三字經','王應麟','宋','蒙學','開蒙要篇','三字經 ，宋王應麟所著，公版全文。','1');
INSERT INTO book_chapter (book_id,chapter_no,chapter_title,content,word_count) VALUES ((SELECT id FROM book WHERE title='三字經' ORDER BY id DESC LIMIT 1),1,'全文','人之初，性本善，性相近，習相遠；
苟不教，性乃遷，教之道，貴以專。
昔孟母，擇隣處，子不學，斷機杼；
竇燕山，有義方，教五子，名俱揚。
養不教，父之過；教不嚴，師之惰。
子不學，非所宜；幼不學，老何為？
玉不琢，不成器；人不學，不知義；
為人子，方少時，親師友，習禮儀。
香九齡，能溫席，孝於親，所當執；
融四歲，能讓梨，弟於長，宜先知。

首孝弟，次見聞，知某數，識某文。
一而十，十而百，百而千，千而萬。
三才者，天地人；三光者，日月星；
三綱者，君臣義，父子親，夫婦別。
曰春夏，曰秋冬，此四時，運不窮；
曰南北，曰西東，此四方，應乎中。
曰水火，木金土，此五行，本乎數；
曰仁義，禮智信，此五常，不容紊。
稻粱菽，麥黍稷，此六穀，人所食；
馬牛羊，雞犬豕，此六畜，人所飼。
曰喜怒，曰哀懼，愛惡慾，七情具；
匏土革，木石金，與絲竹，乃八音。
高曾祖，父而身，身而子，子而孫，自子孫，至曾玄，廼九族，人之倫。
父子恩，夫婦從，兄則友，弟則恭，長幼序，友與朋，君則敬，臣則忠，此十義，人所同。

凡訓蒙，須講究；詳訓詁，明句讀。
為學者，必有初，小學終，至四書：
論語者，二十篇，群弟子，記善言；
孟子者，七篇止，講道德，說仁義；
作中庸，乃孔伋，中不偏，庸不易；
作大學，乃曾子，自脩齊，至平治。

孝經通，四書熟，如六經，始可讀。
詩書易，禮春秋，號六經，當講求：
有連山，有歸藏，有周易，三易詳；
有典謨，有訓誥，有誓命，書之奧；
我姬公，作周禮，著六經，存治體；
大小戴，註禮記，述聖言，禮樂備；
曰國風，曰雅頌，號四詩，當諷詠；
詩既亡，春秋作，寓褒貶，別善惡。
三傳者，有公羊，有左氏，有穀梁；
經既明，方讀子，撮其要，記其事。
五子者，有荀楊。文中子，及老莊。
經子通，讀諸史，考世系，知始終。

自羲農，至黃帝，號三皇，居上世；
唐有虞，號二帝，相揖遜，稱盛治。
夏有禹，商有湯，周文武，稱三王；
夏傳子，家天下，四百載，遷夏社；
湯伐夏，國號商，六百載，至紂亡；
周武王，始誅紂，八百載，最長久。
周轍東，王綱墜，逞干戈，尚游說。
始春秋，終戰國，五霸強，七雄出；
嬴秦氏，始兼并，傳二世，楚漢爭。
高祖興，漢業建，至孝平，王莽篡。
光武興，為東漢，四百年，終於獻；
魏蜀吳，爭峙鼎，號三國，迄兩晉。
宋齊繼，梁陳承，為南朝，都金陵；
北元魏，分東西，宇文周，與高齊；
迨至隋，一土宇，不再傳，失統緒。
唐高祖，起義師，除隋亂，創國基。
二十傳，三百載，梁滅之，國乃改；
梁唐晉，及漢周，稱五代，皆有由。
炎宋興，受周禪，十八傳，胡元混。
十七史，全在茲，載治亂，知興衰；
讀史者，考實錄，通古今，若親目。

口而誦，心而惟，朝於斯，夕於斯。
昔仲尼，師項橐，古聖賢，尚勤學；
趙中令，讀魯論，彼既仕，學且勤；
披蒲編，削竹簡，彼無書，且知勉；
頭懸梁，錐刺股，彼不教，自勤苦；
如囊螢，如映雪，家雖貧，學不輟；
如負薪，如鑿壁，身雖賤，愈自力。
昉四㱓，能咏詩；泌七歲，能賦棋；
彼穎悟，人稱奇，爾幼學，當効之。
蔡文姬，能辨琴；謝道韞，能咏吟；
彼女子，且聰敏，爾男子，當自警。
舉神童，作正字，彼雖幼，身已仕，爾幼學，須知愧。
蘇老泉，二十七，始發憤，讀書籍，
彼既老，猶悔遲，爾小生，宜早思。
若梁灝，八十二，對大廷，魁多士；
彼既成，眾稱異，爾小生，宜立志。

犬守夜，鷄司晨，苟不學，曷為人？
蚕吐絲，蜂釀蜜，人無用，不如物。
幼而學，壯而行，上致君，下澤民；
揚名聲，顯父母，光於前，垂於後。
人遺子，金滿籯，我教子，惟一經；
勤有功，戲無益，戒之哉，宜自勗。

Category:三字經',1512);

