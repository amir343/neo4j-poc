package com.jayway.kperson

import slick.session.{Session, Database}
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable

case class Skill(name:String, level:Int)
case class People(id:Int, firstName:String, lastName:String, skills:List[Skill] = List.empty[Skill])

class KPersonMySql {

  private implicit val getPeopleResult = GetResult( r => People(r.nextInt(), r.nextString(), r.nextString()))
  private implicit val getSkillResult = GetResult( r => Skill(r.nextString(), r.nextInt()))

  private val peopleWithSkills = mutable.ListBuffer[People]()

  Database.forURL(url = "jdbc:mysql://127.0.0.1/kperson", user = "root", password = "", driver = "com.mysql.jdbc.Driver") withSession { implicit session:Session =>

    val people:Iterator[People] = Q.queryNA[People]("SELECT id, first_name, last_name FROM people").elements()

    people foreach { p =>
      peopleWithSkills += p.copy( skills =  getSkills(p))
    }

    def getSkills(people:People):List[Skill] = {
      val skills = mutable.ListBuffer[Skill]()
      Q.queryNA[Skill](s"SELECT name, level FROM skills s JOIN skill_types t ON s.skill_type_id=t.id WHERE s.person_id=${people.id}") foreach {
        s => skills += s
      }
      skills.toList
    }
  }

  val people:List[People] = peopleWithSkills.toList filter { _.skills.size != 0 }

}

object Migration extends App {
  val kPerson = new KPersonMySql()
  kPerson.people foreach println
}
