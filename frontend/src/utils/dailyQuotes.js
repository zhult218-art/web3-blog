// ====================================================
// 每日一句：哲理 / 诗句（含作者），按日期哈希轮播
// ====================================================

// 每条 { text, author }，text 为句子正文，author 为出处/作者
export const DAILY_QUOTES = [
  // —— 哲理格言 ——
  { text: '生活不是等待暴风雨过去，而是学会在雨中起舞。', author: '维维安·格林' },
  { text: '我们仰望同一片星空，却看见不同的世界。', author: '王尔德' },
  { text: '所有的星星都会在某个瞬间同时亮起，那就是我们相遇的时候。', author: '佚名' },
  { text: '你所浪费的今天，是昨天死去的人奢望的明天。', author: '柏拉图' },
  { text: '种一棵树最好的时间是十年前，其次是现在。', author: '非洲谚语' },
  { text: '做自己的光，因为没人会替你照亮整个夜空。', author: '佚名' },
  { text: '山海皆可平，难平是人心。', author: '佚名' },
  { text: '愿你走出半生，归来仍是少年。', author: '苏轼' },
  { text: '生活明朗，万物可爱，人间值得，未来可期。', author: '汪国真' },
  { text: '你若盛开，清风自来。', author: '三毛' },
  // —— 古典诗词 ——
  { text: '星垂平野阔，月涌大江流。', author: '杜甫' },
  { text: '海上生明月，天涯共此时。', author: '张九龄' },
  { text: '春江潮水连海平，海上明月共潮生。', author: '张若虚' },
  { text: '众里寻他千百度，蓦然回首，那人却在，灯火阑珊处。', author: '辛弃疾' },
  { text: '人生若只如初见，何事秋风悲画扇。', author: '纳兰性德' },
  { text: '长风破浪会有时，直挂云帆济沧海。', author: '李白' },
  { text: '且将新火试新茶，诗酒趁年华。', author: '苏轼' },
  { text: '落霞与孤鹜齐飞，秋水共长天一色。', author: '王勃' },
  { text: '醉后不知天在水，满船清梦压星河。', author: '唐温如' },
  { text: '疏影横斜水清浅，暗香浮动月黄昏。', author: '林逋' },
]

// 按日期（年月日）选择一条，保证同一天内固定
export function getDailyQuote() {
  const d = new Date()
  const seed = d.getFullYear() * 10000 + (d.getMonth() + 1) * 100 + d.getDate()
  const idx = seed % DAILY_QUOTES.length
  return DAILY_QUOTES[idx]
}
