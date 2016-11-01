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
      TransitionDef("APPROVING_AI", "DISAPPROVED_AI", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "reject"
      }),
      TransitionDef("APPROVING_AI", "APPROVED_AI", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      }),
      TransitionDef("APPROVING_AI", "APPROVE_ADJUST_AI", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "adjust"
      }),
      TransitionDef("DISAPPROVED_AI", "DISAPPROVED", auto = true),
      TransitionDef("APPROVED_AI", "APPROVED", auto = true),
      TransitionDef("APPROVE_ADJUST_AI", "APPROVING_MANUAL", auto = true),
      TransitionDef("APPROVING_MANUAL", "DISAPPROVED_MANUAL", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "reject"
      }),
      TransitionDef("APPROVING_MANUAL", "APPROVED_MANUAL", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      }),
      TransitionDef("DISAPPROVED_MANUAL", "DISAPPROVED", auto = true),
      TransitionDef("APPROVED_MANUAL", "APPROVED", auto = true),
      TransitionDef("APPROVED", "CONTRACT_GENERATING", auto = false),
      TransitionDef("CONTRACT_GENERATING", "CONTRACT_FAILED", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "error"
      }),
      TransitionDef("CONTRACT_GENERATING", "CONTRACT_READY", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      }),
      TransitionDef("CONTRACT_FAILED", "CONTRACT_GENERATING", auto = false),
      TransitionDef("CONTRACT_READY", "CONTRACT_SIGNING", auto = false),
      TransitionDef("CONTRACT_READY", "BIOPSYING", auto = false),
      TransitionDef("CONTRACT_SIGNING", "CONTRACT_SIGN_FAILED", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "error"
      }),
      TransitionDef("CONTRACT_SIGNING", "CONTRACT_SIGNED", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      }),
      TransitionDef("CONTRACT_SIGN_FAILED", "CONTRACT_SIGNING", auto = false),
      TransitionDef("BIOPSYING", "BIOPSY_FAILED", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "error"
      }),
      TransitionDef("BIOPSYING", "BIOPSY_FINISH", auto = true, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      }),
      TransitionDef("BIOPSY_FAILED", "BIOPSYING", auto = false),
      TransitionDef("BIOPSY_FINISH", "PAYMENT_CONFIRMING", auto = true, {
        _.currStatusCodes.contains("CONTRACT_SIGNED")
      }),
      TransitionDef("CONTRACT_SIGNED", "PAYMENT_CONFIRMING", auto = true, {
        _.currStatusCodes.contains("BIOPSY_FINISH")
      }),
      TransitionDef("PAYMENT_CONFIRMING", "PAYMENT_CONFIRM_REJECTED", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "reject"
      }),
      TransitionDef("PAYMENT_CONFIRMING", "PAYMENT_CONFIRMED", auto = false, {
        _.currArgs("result").asInstanceOf[String] == "pass"
      })
    ))

    Vicuna.start("order", "00001", "测试订单1")

  }

}

