package club.craftcoder.vicuna

import club.craftcoder.vicuna.core.{StatusDef, TransitionDef}
import com.ecfront.common.Resp
import com.ecfront.ez.framework.service.jdbc.JDBCProcessor
import com.ecfront.ez.framework.test.MockStartupSpec

class VicunaSpec extends MockStartupSpec {

  test("basic test") {
    JDBCProcessor.ddl("TRUNCATE TABLE vic_status_log")
    Vicuna.setDataExchange(TestDataExchange).define("order", "订单", List(
      StatusDef("SUBMITTED", "提交", {
        inst =>
          inst.currArgs += "down_payment" -> BigDecimal(111.1)
          inst.currArgs += "final_payment" -> BigDecimal(222.1)
          Resp.success(null)
      }),

      StatusDef("APPROVING_AI", "机审中", { _ => Resp.success(null) }),
      StatusDef("DISAPPROVED_AI", "机审拒绝", { _ => Resp.success(null) }),
      StatusDef("APPROVE_ADJUST_AI", "机审调整", {
        inst =>
          inst.currArgs += "down_payment" -> BigDecimal(222.1)
          inst.currArgs += "final_payment" -> BigDecimal(333.1)
          Resp.success(null)
      }),
      StatusDef("APPROVED_AI", "机审通过", { _ => Resp.success(null) }),
      StatusDef("APPROVING_MANUAL", "人工审核中", { _ => Resp.success(null) }),
      StatusDef("DISAPPROVED_MANUAL", "人工审核拒绝", { _ => Resp.success(null) }),
      StatusDef("APPROVED_MANUAL", "人工审核通过", { _ => Resp.success(null) }),
      StatusDef("DISAPPROVED", "审核拒绝", { _ => Resp.success(null) }),
      StatusDef("APPROVED", "审核通过", { _ => Resp.success(null) }),

      StatusDef("CONTRACT_GENERATING", "合同生成中", { _ => Resp.success(null) }),
      StatusDef("CONTRACT_FAILED", "合同生成失败", { _ => Resp.success(null) }),
      StatusDef("CONTRACT_READY", "合同已生成", { _ => Resp.success(null) }),

      StatusDef("CONTRACT_SIGNING", "签约中", { _ => Resp.success(null) }),
      StatusDef("CONTRACT_SIGN_FAILED", "签约失败", { _ => Resp.success(null) }),
      StatusDef("CONTRACT_SIGNED", "已签约", { _ => Resp.success(null) }),

      StatusDef("BIOPSYING", "活体比对中", { _ => Resp.success(null) }),
      StatusDef("BIOPSY_FAILED", "活体比对失败", { _ => Resp.success(null) }),
      StatusDef("BIOPSY_FINISH", "活体比对完成", { _ => Resp.success(null) }),

      StatusDef("PAYMENT_CONFIRMING", "放款确认中", { _ => Resp.success(null) }),
      StatusDef("PAYMENT_CONFIRM_REJECTED", "放款确认拒绝", { _ => Resp.success(null) }),
      StatusDef("PAYMENT_CONFIRMED", "放款完成", { _ => Resp.success(null) }),

      StatusDef("CANCEL", "取消", { _ => Resp.success(null) })
    ), List(
      TransitionDef("SUBMITTED", "APPROVING_AI", auto = true),
      TransitionDef("APPROVING_AI", "DISAPPROVED_AI", auto = false, _.currArgs("result").asInstanceOf[String] == "reject",name ="机审拒绝"),
      TransitionDef("APPROVING_AI", "APPROVED_AI", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="机审通过"),
      TransitionDef("APPROVING_AI", "APPROVE_ADJUST_AI", auto = false, _.currArgs("result").asInstanceOf[String] == "adjust",name ="机审调整"),
      TransitionDef("DISAPPROVED_AI", "DISAPPROVED", auto = true),
      TransitionDef("APPROVED_AI", "APPROVED", auto = true),
      TransitionDef("APPROVE_ADJUST_AI", "APPROVING_MANUAL", auto = true),
      TransitionDef("APPROVING_MANUAL", "DISAPPROVED_MANUAL", auto = false, _.currArgs("result").asInstanceOf[String] == "reject",name ="终审拒绝"),
      TransitionDef("APPROVING_MANUAL", "APPROVED_MANUAL", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="终审通过"),
      TransitionDef("DISAPPROVED_MANUAL", "DISAPPROVED", auto = true),
      TransitionDef("APPROVED_MANUAL", "APPROVED", auto = true),
      TransitionDef("APPROVED", "CONTRACT_GENERATING", auto = false),
      TransitionDef("CONTRACT_GENERATING", "CONTRACT_FAILED", auto = false, _.currArgs("result").asInstanceOf[String] == "error",name ="生成失败"),
      TransitionDef("CONTRACT_GENERATING", "CONTRACT_READY", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="生成完成"),
      TransitionDef("CONTRACT_FAILED", "CONTRACT_GENERATING", auto = false),
      TransitionDef("CONTRACT_READY", "CONTRACT_SIGNING", auto = true),
      TransitionDef("CONTRACT_READY", "BIOPSYING", auto = true),
      TransitionDef("CONTRACT_SIGNING", "CONTRACT_SIGN_FAILED", auto = false, _.currArgs("result").asInstanceOf[String] == "error",name ="签约失败"),
      TransitionDef("CONTRACT_SIGNING", "CONTRACT_SIGNED", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="签约完成"),
      TransitionDef("CONTRACT_SIGN_FAILED", "CONTRACT_SIGNING", auto = false,name ="重新签约"),
      TransitionDef("BIOPSYING", "BIOPSY_FAILED", auto = false, _.currArgs("result").asInstanceOf[String] == "error",name ="活检失败"),
      TransitionDef("BIOPSYING", "BIOPSY_FINISH", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="活检通过"),
      TransitionDef("BIOPSY_FAILED", "BIOPSYING", auto = false,name ="重新活检"),
      TransitionDef("BIOPSY_FINISH", "PAYMENT_CONFIRMING", auto = true, _.currStatusCodes.contains("CONTRACT_SIGNED"),name ="合同已签约，尝试付款"),
      TransitionDef("CONTRACT_SIGNED", "PAYMENT_CONFIRMING", auto = true, _.currStatusCodes.contains("BIOPSY_FINISH"),name ="活检已完成，尝试付款"),
      TransitionDef("PAYMENT_CONFIRMING", "PAYMENT_CONFIRM_REJECTED", auto = false, _.currArgs("result").asInstanceOf[String] == "reject",name ="付款拒绝"),
      TransitionDef("PAYMENT_CONFIRMING", "PAYMENT_CONFIRMED", auto = false, _.currArgs("result").asInstanceOf[String] == "pass",name ="付款完成")
    ))

    Vicuna.buildChart("order", "C:\\Users\\i\\OneDrive\\Work_Projects\\ProjectDeveloping\\Vicuna\\src\\test\\resources\\order_chart.html")

    val t1 = new Thread(new Runnable {
      override def run(): Unit = {
        Vicuna.start("order", "00001", "测试订单1")
        Thread.sleep(1000)
        Vicuna.next("order", "APPROVING_AI", "00001", Map("result" -> "reject"))
      }
    })
    val t2 = new Thread(new Runnable {
      override def run(): Unit = {
        Vicuna.start("order", "00002", "测试订单2")
        Thread.sleep(1000)
        Vicuna.next("order", "APPROVING_AI", "00002", Map("result" -> "adjust"))
        Thread.sleep(1000)
        Vicuna.next("order", "APPROVING_MANUAL", "00002", Map("result" -> "pass"))
        Thread.sleep(1000)
        Vicuna.next("order", "APPROVED", "00002")
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_GENERATING", "00002", Map("result" -> "error"))
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_FAILED", "00002")
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_GENERATING", "00002", Map("result" -> "pass"))
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_SIGNING", "00002", Map("result" -> "error"))
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_SIGN_FAILED", "00002")
        Thread.sleep(1000)
        Vicuna.next("order", "CONTRACT_SIGNING", "00002", Map("result" -> "pass"))
        Thread.sleep(1000)
        Vicuna.next("order", "BIOPSYING", "00002", Map("result" -> "pass"))
        Thread.sleep(1000)
        Vicuna.next("order", "PAYMENT_CONFIRMING", "00002", Map("result" -> "pass"))
        Thread.sleep(1000)
      }
    })
    val t3 = new Thread(new Runnable {
      override def run(): Unit = {
        Vicuna.start("order", "00003", "测试订单3")
        Thread.sleep(1000)
        assert(!Vicuna.go("order", "APPROVED_AI", "00003", Map("result" -> "adjust")))
        assert(!Vicuna.next("order", "APPROVED_AI", "00003", Map("result" -> "pass")))
        assert(Vicuna.go("order", "APPROVED_AI", "00003", Map("result" -> "pass")))
      }
    })
    val t4 = new Thread(new Runnable {
      override def run(): Unit = {
      }
    })
    t1.start()
    t2.start()
    t3.start()
    t4.start()
    t1.join()
    t2.join()
    t3.join()
    t4.join()

  }

}

