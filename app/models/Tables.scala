package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = scala.slick.driver.MySQLDriver
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: scala.slick.driver.JdbcProfile
  import profile.simple._
  import scala.slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import scala.slick.jdbc.{GetResult => GR}
  
  /** DDL for all tables. Call .create to execute. */
  lazy val ddl = FollowTable.ddl ++ MemberTable.ddl
  
  /** Entity class storing rows of table FollowTable
   *  @param id Database column ID DBType(VARCHAR), PrimaryKey, Length(20,true)
   *  @param memberId Database column MEMBER_ID DBType(VARCHAR), Length(30,true) */
  case class FollowTableRow(id: String, memberId: String)
  /** GetResult implicit for fetching FollowTableRow objects using plain SQL queries */
  implicit def GetResultFollowTableRow(implicit e0: GR[String]): GR[FollowTableRow] = GR{
    prs => import prs._
    FollowTableRow.tupled((<<[String], <<[String]))
  }
  /** Table description of table FOLLOW_TABLE. Objects of this class serve as prototypes for rows in queries. */
  class FollowTable(_tableTag: Tag) extends Table[FollowTableRow](_tableTag, "FOLLOW_TABLE") {
    def * = (id, memberId) <> (FollowTableRow.tupled, FollowTableRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (id.?, memberId.?).shaped.<>({r=>import r._; _1.map(_=> FollowTableRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    
    /** Database column ID DBType(VARCHAR), PrimaryKey, Length(20,true) */
    val id: Column[String] = column[String]("ID", O.PrimaryKey, O.Length(20,varying=true))
    /** Database column MEMBER_ID DBType(VARCHAR), Length(30,true) */
    val memberId: Column[String] = column[String]("MEMBER_ID", O.Length(30,varying=true))
    
    /** Foreign key referencing MemberTable (database name follow_table_ibfk_1) */
    lazy val memberTableFk = foreignKey("follow_table_ibfk_1", memberId, MemberTable)(r => r.memberId, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
    
    /** Uniqueness Index over (memberId) (database name MEMBER_ID) */
    val index1 = index("MEMBER_ID", memberId, unique=true)
  }
  /** Collection-like TableQuery object for table FollowTable */
  lazy val FollowTable = new TableQuery(tag => new FollowTable(tag))
  
  /** Entity class storing rows of table MemberTable
   *  @param memberId Database column MEMBER_ID DBType(VARCHAR), PrimaryKey, Length(30,true)
   *  @param memberName Database column MEMBER_NAME DBType(VARCHAR), Length(30,true), Default(None)
   *  @param memberPassword Database column MEMBER_PASSWORD DBType(VARCHAR), Length(20,true)
   *  @param memberMailAddress Database column MEMBER_MAIL_ADDRESS DBType(VARCHAR), Length(40,true)
   *  @param timestampCreated Database column TIMESTAMP_CREATED DBType(TIMESTAMP)
   *  @param timestampUpdated Database column TIMESTAMP_UPDATED DBType(TIMESTAMP) */
  case class MemberTableRow(memberId: String, memberName: Option[String] = None, memberPassword: String, memberMailAddress: String, timestampCreated: java.sql.Timestamp, timestampUpdated: java.sql.Timestamp)
  /** GetResult implicit for fetching MemberTableRow objects using plain SQL queries */
  implicit def GetResultMemberTableRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.sql.Timestamp]): GR[MemberTableRow] = GR{
    prs => import prs._
    MemberTableRow.tupled((<<[String], <<?[String], <<[String], <<[String], <<[java.sql.Timestamp], <<[java.sql.Timestamp]))
  }
  /** Table description of table MEMBER_TABLE. Objects of this class serve as prototypes for rows in queries. */
  class MemberTable(_tableTag: Tag) extends Table[MemberTableRow](_tableTag, "MEMBER_TABLE") {
    def * = (memberId, memberName, memberPassword, memberMailAddress, timestampCreated, timestampUpdated) <> (MemberTableRow.tupled, MemberTableRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (memberId.?, memberName, memberPassword.?, memberMailAddress.?, timestampCreated.?, timestampUpdated.?).shaped.<>({r=>import r._; _1.map(_=> MemberTableRow.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))
    
    /** Database column MEMBER_ID DBType(VARCHAR), PrimaryKey, Length(30,true) */
    val memberId: Column[String] = column[String]("MEMBER_ID", O.PrimaryKey, O.Length(30,varying=true))
    /** Database column MEMBER_NAME DBType(VARCHAR), Length(30,true), Default(None) */
    val memberName: Column[Option[String]] = column[Option[String]]("MEMBER_NAME", O.Length(30,varying=true), O.Default(None))
    /** Database column MEMBER_PASSWORD DBType(VARCHAR), Length(20,true) */
    val memberPassword: Column[String] = column[String]("MEMBER_PASSWORD", O.Length(20,varying=true))
    /** Database column MEMBER_MAIL_ADDRESS DBType(VARCHAR), Length(40,true) */
    val memberMailAddress: Column[String] = column[String]("MEMBER_MAIL_ADDRESS", O.Length(40,varying=true))
    /** Database column TIMESTAMP_CREATED DBType(TIMESTAMP) */
    val timestampCreated: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("TIMESTAMP_CREATED")
    /** Database column TIMESTAMP_UPDATED DBType(TIMESTAMP) */
    val timestampUpdated: Column[java.sql.Timestamp] = column[java.sql.Timestamp]("TIMESTAMP_UPDATED")
  }
  /** Collection-like TableQuery object for table MemberTable */
  lazy val MemberTable = new TableQuery(tag => new MemberTable(tag))
}