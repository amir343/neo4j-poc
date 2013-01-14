package com.jayway.kperson

import slick.session.{Session, Database}
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.collection.mutable
import org.neo4j.rest.graphdb.RestGraphDatabase
import org.neo4j.graphdb.{RelationshipType, Node}

sealed abstract class PersonNode {
  def name:String
}
case class Skill(name:String, level:Int, future_level:Int) extends PersonNode
case class People(id:Int, firstName:String, lastName:String, skills:List[Skill] = List.empty[Skill]) extends PersonNode {
  override val name:String = s"$firstName $lastName"
}

object KNOWS extends RelationshipType {
  val name = "KNOWS"
}

object WANT_TO_KNOW extends RelationshipType {
  val name = "WANT_TO_KNOW"
}


class KPersonMySql {

  private implicit val getPeopleResult = GetResult( r => People(r.nextInt(), r.nextString(), r.nextString()))
  private implicit val getSkillResult = GetResult( r => Skill(r.nextString(), r.nextInt(), r.nextInt()))

  private val peopleWithSkills = mutable.ListBuffer[People]()

  Database.forURL(url = "jdbc:mysql://127.0.0.1/kperson", user = "root", password = "", driver = "com.mysql.jdbc.Driver") withSession { implicit session:Session =>

    val people:Iterator[People] = Q.queryNA[People]("SELECT id, first_name, last_name FROM people").elements()

    people foreach { p =>
      peopleWithSkills += p.copy( skills = getSkills(p))
    }

    def getSkills(people:People):List[Skill] = {
      val skills = mutable.ListBuffer[Skill]()
      Q.queryNA[Skill](s"SELECT name, level, future_level FROM skills s JOIN skill_types t ON s.skill_type_id=t.id WHERE s.person_id=${people.id}") foreach {
        s => skills += s
      }
      skills.toList
    }
  }

  val people:List[People] = peopleWithSkills.toList filter { _.skills.size != 0 }

}

class NeoMigration {

  val g = new RestGraphDatabase("http://localhost:7474/db/data")
  val nodes = mutable.HashMap[String, Node]()

  def migrate(employees:List[People]) {
    employees foreach { emp =>
      val empNode = createNode(emp)
      emp.skills foreach { skill =>
        if ( skill.level > 0  || skill.future_level > 0) {
          val skillNode = createNode(skill)
          if (skill.level > 0) {
            val rel = empNode.createRelationshipTo(skillNode, KNOWS)
            rel.setProperty("level", skill.level.intValue())
          }
          if (skill.future_level > 0) {
            val rel2 = empNode.createRelationshipTo(skillNode, WANT_TO_KNOW)
            rel2.setProperty("level", skill.future_level.intValue())
          }
        }
      }
    }
  }

  private def createNode(personNode:PersonNode):Node = {
    nodes.get(personNode.name) match {
      case None =>
        val node = g.createNode()
        node.setProperty("name", personNode.name)
        nodes += personNode.name -> node
        node
      case Some(n) =>
        n
    }
  }


}


object Migration extends App {
  val kPerson = new KPersonMySql()
  val people = kPerson.people
  val neoMigration = new NeoMigration
  neoMigration.migrate(people)
}
