package club.craftcoder.vicuna

import club.craftcoder.Vicuna
import club.craftcoder.vicuna.core.{StatusDef, TransferDef}
import com.ecfront.common.Resp
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.scalatest.{BeforeAndAfter, FunSuite}

class VicunaSpec extends FunSuite with BeforeAndAfter with LazyLogging {

  test("basic test") {
      Vicuna.define("order","订单",List(
        StatusDef("SUBMITTED","提交",{
          (inst,order) =>
            inst.args+= "tmp1" -> "1"
            Resp.success(null)
        }),

        StatusDef("APPROVING_AI","机审中",{(inst,_) => Resp.success(null)}),
        StatusDef("DISAPPROVED_AI","机审拒绝",{(inst,_) => Resp.success(null)}),
        StatusDef("APPROVE_ADJUST_AI","机审调整",{(inst,_) => Resp.success(null)}),
        StatusDef("APPROVED_AI","机审通过",{(inst,_) => Resp.success(null)}),
        StatusDef("APPROVING_MANUAL","人工审核中",{(inst,_) => Resp.success(null)}),
        StatusDef("DISAPPROVED_MANUAL","人工审核拒绝",{(inst,_) => Resp.success(null)}),
        StatusDef("APPROVED_MANUAL","人工审核通过",{(inst,_) => Resp.success(null)}),
        StatusDef("DISAPPROVED","审核拒绝",{(inst,_) => Resp.success(null)}),
        StatusDef("APPROVED","审核通过",{(inst,_) => Resp.success(null)}),

        StatusDef("CONTRACT_GENERATING","合同生成中",{(inst,_) => Resp.success(null)}),
        StatusDef("CONTRACT_FAILED","合同生成失败",{(inst,_) => Resp.success(null)}),
        StatusDef("CONTRACT_READY","合同已生成",{(inst,_) => Resp.success(null)}),

        StatusDef("CONTRACT_SIGNING","签约中",{(inst,_) => Resp.success(null)}),
        StatusDef("CONTRACT_SIGN_FAILED","签约失败",{(inst,_) => Resp.success(null)}),
        StatusDef("CONTRACT_SIGNED","已签约",{(inst,_) => Resp.success(null)}),

        StatusDef("BIOPSYING","活体比对中",{(inst,_) => Resp.success(null)}),
        StatusDef("BIOPSY_FAILED","活体比对失败",{(inst,_) => Resp.success(null)}),
        StatusDef("BIOPSY_FINISH","活体比对完成",{(inst,_) => Resp.success(null)}),

        StatusDef("PAYMENT_CONFIRMING","放款确认中",{(inst,_) => Resp.success(null)}),
        StatusDef("PAYMENT_CONFIRM_REJECTED","放款确认拒绝",{(inst,_) => Resp.success(null)}),
        StatusDef("PAYMENT_CONFIRMED","放款完成",{(inst,_) => Resp.success(null)}),

        StatusDef("CANCEL","取消",{(inst,_) => Resp.success(null)})
      ),List(
        TransferDef("SUBMITTED","APPROVING_AI",auto = true),
        TransferDef("APPROVING_AI","DISAPPROVED_AI",auto = false,{_.args("result").asInstanceOf[String]=="reject"}),
        TransferDef("APPROVING_AI","APPROVED_AI",auto = false,{_.args("result").asInstanceOf[String]=="pass"}),
        TransferDef("APPROVING_AI","APPROVE_ADJUST_AI",auto = false,{_.args("result").asInstanceOf[String]=="adjust"}),
        TransferDef("DISAPPROVED_AI","DISAPPROVED",auto = true),
        TransferDef("APPROVED_AI","APPROVED",auto = true),
        TransferDef("APPROVE_ADJUST_AI","APPROVING_MANUAL",auto = true),
        TransferDef("APPROVING_MANUAL","DISAPPROVED_MANUAL",auto = false,{_.args("result").asInstanceOf[String]=="reject"}),
        TransferDef("APPROVING_MANUAL","APPROVED_MANUAL",auto = false,{_.args("result").asInstanceOf[String]=="pass"}),
        TransferDef("DISAPPROVED_MANUAL","DISAPPROVED",auto = true),
        TransferDef("APPROVED_MANUAL","APPROVED",auto = true),
        TransferDef("APPROVED","CONTRACT_GENERATING",auto = false),
        TransferDef("CONTRACT_GENERATING","CONTRACT_FAILED",auto = true,{_.args("result").asInstanceOf[String]=="error"}),
        TransferDef("CONTRACT_GENERATING","CONTRACT_READY",auto = true,{_.args("result").asInstanceOf[String]=="pass"}),
        TransferDef("CONTRACT_FAILED","CONTRACT_GENERATING",auto = false),
        TransferDef("CONTRACT_READY","CONTRACT_SIGNING",auto = false),
        TransferDef("CONTRACT_READY","BIOPSYING",auto = false),
        TransferDef("CONTRACT_SIGNING","CONTRACT_SIGN_FAILED",auto = true,{_.args("result").asInstanceOf[String]=="error"}),
        TransferDef("CONTRACT_SIGNING","CONTRACT_SIGNED",auto = true,{_.args("result").asInstanceOf[String]=="pass"}),
        TransferDef("CONTRACT_SIGN_FAILED","CONTRACT_SIGNING",auto = false),
        TransferDef("BIOPSYING","BIOPSY_FAILED",auto = true,{_.args("result").asInstanceOf[String]=="error"}),
        TransferDef("BIOPSYING","BIOPSY_FINISH",auto = true,{_.args("result").asInstanceOf[String]=="pass"}),
        TransferDef("BIOPSY_FINISH","BIOPSYING",auto = false),
        TransferDef("CONTRACT_SIGNED","PAYMENT_CONFIRMING",auto = true),
        TransferDef("PAYMENT_CONFIRMING","PAYMENT_CONFIRM_REJECTED",auto = false,{_.args("result").asInstanceOf[String]=="reject"}),
        TransferDef("PAYMENT_CONFIRMING","PAYMENT_CONFIRMED",auto = false,{_.args("result").asInstanceOf[String]=="pass"})
      ))

    Vicuna.start("order","00001","测试订单1",Map("someData" -> "haha"))

  }

}

