package com.xxl.job.admin.core.alarm.impl;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ：weimin
 * @date ：2020/10/27
 * @description：ding告警
 */
@Component
public class DingJobAlarm implements JobAlarm {
    private static Logger logger = LoggerFactory.getLogger(DingJobAlarm.class);

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        boolean alarmResult = true;
        String dingWebhook = XxlJobAdminConfig.getAdminConfig().getDingWebhook();
        if (!StringUtils.isEmpty(dingWebhook)) {
            try {
                String alarmContent = "Alarm Job LogId : " + jobLog.getId();
                String msg = jobLog.getTriggerMsg();
                if (jobLog.getTriggerCode() != ReturnT.SUCCESS_CODE && !StringUtils.isEmpty(msg)) {
                    msg = msg.lastIndexOf("msg：")==-1?msg:msg.substring(msg.lastIndexOf("msg：")+4);
                    alarmContent += " , TriggerMsg : " + msg;
                }
                if (jobLog.getHandleCode()>0 && jobLog.getHandleCode() != ReturnT.SUCCESS_CODE) {
                    alarmContent += " , HandleCode : \n" + jobLog.getHandleMsg();
                }
                XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(Integer.valueOf(info.getJobGroup()));
                String content = loadDingAlarmTemplate(group != null ? group.getTitle() : "null",
                        jobLog.getExecutorAddress(),
                        info.getId(),
                        info.getJobDesc(),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                        alarmContent);
                DingTalkClient client = new DefaultDingTalkClient(dingWebhook);
                OapiRobotSendRequest request = new OapiRobotSendRequest();
                request.setMsgtype("text");
                OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
                text.setContent(content);
                request.setText(text);
                client.execute(request);
            } catch (Exception e) {
                logger.error(">>>>>>>>>>> xxl-job, job fail alarm ding send error, JobLogId:{}", jobLog.getId(), e);
                alarmResult = false;
            }
        }
        return alarmResult;
    }

    /**
     * load ding job alarm template
     *
     * @return
     */
    private static final String loadDingAlarmTemplate(String executor,String executorIp, int jobId, String jobDesc, String jobTime, String content) {
        return "监控告警明细 \n" +
                " >-执行器 : " + executor + "\n" +
                " >-执行器IP : " + executorIp + "\n" +
                " >-任务ID : " + jobId + "\n" +
                " >-任务描述 : " + jobDesc + "\n" +
                " >-告警类型 : " + I18nUtil.getString("jobconf_monitor_alarm_type") + "\n" +
                " >-执行任务时间: " + jobTime + "\n" +
                " >-告警内容 : [" + content + "]\n";
    }
}
