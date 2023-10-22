package cz.pyramidak.kalendar

import cz.pyramidak.kalendar.firebase.Message
import cz.pyramidak.kalendar.room.Osoba
import cz.pyramidak.kalendar.room.Upominka
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Person {
    var jmeno: String = ""
    var rodne: String? = null
    var narozeni: LocalDate? = null
    var umrti: LocalDate? = null
    var zmena: LocalDateTime = LocalDateTime.now()
    var smazane: Boolean = false
    var uid: Int = 0
    constructor() : super()
    constructor(osoba: Osoba) {
        jmeno = osoba.jmeno ?:""
        rodne = osoba.rodne
        narozeni = osoba.narozeni.toDate()!!
        umrti = osoba.umrti.toDate()
        zmena = osoba.zmena.toDateTime()!!
        smazane = osoba.smazane ?:false
        uid = osoba.uid ?:0
    }
}

class Plan {
    var vznik: LocalDateTime = LocalDateTime.now()
    var den: LocalDateTime = LocalDateTime.now()
    var text: String = ""
    var mesicne: Boolean = false
    var rocne: Boolean = false
    var zmena: LocalDateTime = LocalDateTime.now()
    var alarm: LocalDateTime? = null
    var uid: Int = 0
    constructor() : super()
    constructor(day: LocalDateTime) { den = day }
    constructor(upominka: Upominka) {
        vznik = upominka.vznik.toDateTime()!!
        den = upominka.den.toDateTime()!!
        text = upominka.text ?:""
        mesicne = upominka.mesicne ?:false
        rocne = upominka.rocne ?:false
        zmena = upominka.zmena.toDateTime()!!
        alarm = upominka.alarm.toDateTime()
        uid = upominka.uid ?:0
    }
}

class Day {
    var date: LocalDate = LocalDate.now()
    var personsOfNameday: MutableList<Person> = mutableListOf()
    var personsOfBirthday: MutableList<Person> = mutableListOf()
    var persons: MutableList<Person> = mutableListOf()
    var plans: MutableList<Plan> = mutableListOf()
    var edit: Tables = Tables.PERSON
}

enum class Tables {
    PERSON, PLAN, ALL, MESSAGE
}

class Found {
    var person: Person? = null
    var plan: Plan? = null
    var icon: Int = 0
    var name: String = ""
    var date: LocalDate = LocalDate.now()
    var selected: Boolean = false
    var sender: String? = null

    constructor(person: Person) {
        this.person = person
        icon = R.drawable.ic_person
        name = person.jmeno
        date = person.narozeni!!
    }
    constructor(plan: Plan) {
        this.plan = plan
        icon = R.drawable.ic_note
        name = plan.text
        date = plan.den.toLocalDate()
    }
    constructor(message: Message) {
        this.sender = message.sender
        if (message.jmeno != null) {
            icon = R.drawable.ic_person
            name = message.jmeno!!
            date = message.narozeni.toDate()!!
            person = Person().apply {
                jmeno = name
                narozeni = date
                umrti = message.umrti.toDate()
            }
        } else {
            icon = R.drawable.ic_note
            name = message.text!!
            date = message.den.toDate()!!
            plan = Plan().apply {
                text ="<${message.sender}> $name"
                den = message.den.toDateTime()!!
                mesicne = message.mesicne ?:false
                rocne = message.rocne ?:false
            }
        }
    }
}

fun String?.toDateTime(): LocalDateTime? {
    if (this == null) return null
    if (this == "" || this.startsWith("0001-01-01")) return null
    var datetime: LocalDateTime? = null
    try {
        datetime = LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    } catch (e: Exception) {
    }
    return datetime
}

fun String?.toDate(): LocalDate? {
    return this.toDateTime()?.toLocalDate() ?: return null
}

fun LocalDateTime?.dbString(): String {
    return this?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) ?:""
}

fun LocalDate?.dbString(): String {
    return if (this == null) {
        ""
    } else {
        val date = LocalDateTime.of(this.year,this.monthValue,this.dayOfMonth,0,0)
        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    }
}

fun LocalDate?.csString(): String {
    return this?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?:""
}

fun LocalDateTime?.csString(): String {
    return this?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) ?:""
}


