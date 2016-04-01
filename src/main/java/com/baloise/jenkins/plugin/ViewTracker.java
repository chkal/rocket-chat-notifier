package com.baloise.jenkins.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.View;
import jenkins.model.Jenkins;

public class ViewTracker {
	
	Logger LOG = Logger.getLogger(getClass().getName());
	
	Map<String, Result> results = new HashMap<>();
	List<ViewListener> listeners = new ArrayList<>();
	
	private Collection<View> getViewsAffectedBy(Run<?, ?> run) {
		Collection<View> views = Jenkins.getInstance().getViews();
		List<View> affected = new ArrayList<>(views.size());
		Job<?, ?> parent = run.getParent();
		if (parent instanceof TopLevelItem) {
			for (View view : views) {
				if (view.contains((TopLevelItem) parent)) {
					affected.add(view);
				}
			}
		} else {
			LOG.log(Level.WARNING, parent.getClass() + " not instanceof TopLevelItem");
		}
		return affected;
	}

	public ViewTracker addViewListener(ViewListener listener) {
		listeners.add(listener);
		return this;
	}
	
	public void trackViews(Run<?, ?> run) {
		Collection<View> affectedViews = getViewsAffectedBy(run);
		if (run.isBuilding()) {
			ensureInitialState(affectedViews);
		} else {
			fireDifference(affectedViews);
		}
	}

	private void fireDifference(Collection<View> affectedViews) {
		for (View view : affectedViews) {
			String key = key(view);
			Result newResult = getResult(view);
			Result oldResult = results.get(key);
			if(oldResult != null && !oldResult.equals(newResult)) {
				results.put(key, newResult);
				fireViewChanged(view, oldResult, newResult);
			}
		}
	}

	private void fireViewChanged(View view, Result oldResult, Result newResult) {
		for (ViewListener viewListener : listeners) {
			viewListener.fireViewChanged(view, oldResult, newResult);
		}
	}

	private Result getResult(View view) {
		Result ret = Result.SUCCESS;
		for (TopLevelItem item : view.getAllItems()) {
			for (Job<?,?> job : item.getAllJobs()) {
				Run<?, ?> build = job.getLastCompletedBuild();
				if(build != null) {
					Result result = build.getResult();
					if(result.isBetterOrEqualTo(Result.FAILURE)) 
						ret = ret.combine(result);
				}
			}
		}
		return ret;
	}

	private String key(View view) {
		return view.getDisplayName();
	}

	private void ensureInitialState(Collection<View> affectedViews) {
		for (View view : affectedViews) {
			String key = key(view);
			if (!results.containsKey(key)) {
				results.put(key, getResult(view));
			}
		}
	}

	public void disable() {
		results.clear();
	}
}
