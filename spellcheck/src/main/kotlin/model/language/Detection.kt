package model.language

fun detectLanguage(word: String): Language {
  var hasRus = false
  var hasEng = false
  var hasOther = false
  for (letter in word) {
    if ((letter in ('a'..'z')) || (letter in ('A'..'Z'))) {
      hasEng = true
    } else if ((letter in ('а'..'я')) || (letter in ('А'..'Я')) || (letter == 'ё') || (letter == 'Ё')) {
      hasRus = true
    } else {
      hasOther = true
    }
  }
  return if (hasRus && !hasEng && !hasOther) {
    Language.RUS
  } else if (!hasRus && hasEng && !hasOther) {
    Language.ENG
  } else {
    Language.OTHER
  }
}
