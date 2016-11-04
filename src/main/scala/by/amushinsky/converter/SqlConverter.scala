package by.amushinsky.converter

import scala.collection.JavaConversions._
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.util.TablesNamesFinder
import net.sf.jsqlparser.util.deparser.SelectDeParser
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.expression.BinaryExpression
import net.sf.jsqlparser.expression.operators.relational.EqualsTo


object SqlConverter extends App {
  
  case class LeftJoin(from: Array[String], on: Array[String], fromField: String, onField: String, limit: Int)
  
  def parseLeftJoin(leftJoin: String): LeftJoin = {
    val select = CCJSqlParserUtil.parse(leftJoin.replace('/', '_')).asInstanceOf[Select]
    val plainSelect = select.getSelectBody.asInstanceOf[PlainSelect]
    val from = plainSelect.getFromItem.toString.split("_")
    val join = plainSelect.getJoins.get(0)
    val on = join.getRightItem.toString.split("_")
    val expr = join.getOnExpression.asInstanceOf[BinaryExpression]
    val fromField = expr.getLeftExpression.toString
    val onField = expr.getRightExpression.toString
    val limit = Option(plainSelect.getLimit).map { _.getRowCount.toInt }.getOrElse(10)
    LeftJoin(from, on, fromField, onField, limit)
  }
}