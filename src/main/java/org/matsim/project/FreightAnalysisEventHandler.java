package org.matsim.project;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.events.handler.EventHandler;

public class FreightAnalysisEventHandler implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

    public void handleEvent(IterationEndsEvent e){
        System.out.println("GuNa!");
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {

    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

    }

    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {

    }
}
