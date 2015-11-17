package com.inMind.inMindAgent;

import android.os.Handler;
import android.util.Log;

import com.yahoo.inmind.comm.generic.control.MessageBroker;
import com.yahoo.inmind.comm.generic.model.MBRequest;
import com.yahoo.inmind.commons.control.Constants;
import com.yahoo.inmind.commons.control.Util;
import com.yahoo.inmind.commons.rules.control.DecisionRuleValidator;
import com.yahoo.inmind.commons.rules.model.DecisionRule;

import InMind.Consts;

public class MessageController
{
    static Integer currentRuleId = 0;
    static Class className = MessageController.class;

    public static void dealWithMessage(String command, String args, Handler talkHandler)
    {
        if (command.equalsIgnoreCase(Consts.news))
        {
            NewsCommunicator.dealWithMessage(args, talkHandler);
        }
        else if (command.equalsIgnoreCase(Consts.execJson))
        {
            Log.d("dealWithMessage", "json, executing rule with middleware. args:" + args);
            MessageBroker.getExistingInstance(className).send(className,
                    MBRequest.build(Constants.MSG_CREATE_DECISION_RULE)
                            .put(Constants.DECISION_RULE_JSON, args)
                            .put(Constants.DECISION_RULE_ID, currentRuleId.toString()));
            currentRuleId++;
        }
    }

}
