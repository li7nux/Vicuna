package club.craftcoder.vicuna

import com.ecfront.ez.framework.service.jdbc._

import scala.beans.BeanProperty

@Entity("状态日志")
class Vic_status_log extends SecureModel with StatusModel {
  @UUID
  @BeanProperty var vic_status_uuid: String = _
  @Desc("流程编码", 200, 0)
  @Index
  @BeanProperty var flow_code: String = _
  @Desc("主体编码", 200, 0)
  @Index
  @BeanProperty var obj_code: String = _
  @Desc("状态", 200, 0)
  @Index
  @BeanProperty var status: String = _
  @Desc("进入时间", 0, 0)
  @Index
  @BeanProperty var enter_time: Long = _
  @Desc("离开时间", 0, 0)
  @Index
  @BeanProperty var leave_time: Long = _
  @Desc("首付金额", 14, 2)
  @BeanProperty var down_payment: BigDecimal = _
  @Desc("尾款金额", 14, 2)
  @BeanProperty var final_payment: BigDecimal = _

}

object Vic_status_log extends SecureStorage[Vic_status_log] with StatusStorage[Vic_status_log]













