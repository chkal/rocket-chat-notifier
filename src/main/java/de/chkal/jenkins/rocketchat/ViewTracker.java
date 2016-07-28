package de.chkal.jenkins.rocketchat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    TopLevelItem parent = getTopLevelParent(run);
    if (parent != null) {
      for (View view : views) {
        if (view.contains(parent)) {
          affected.add(view);
        }
      }
    } else {
      LOG.log(Level.WARNING, run.getParent().getClass() + " not instanceof TopLevelItem");
    }
    return affected;
  }

  private TopLevelItem getTopLevelParent(Object o) {
    if (o == null) return null;
    if (o instanceof TopLevelItem) return (TopLevelItem) o;
    Object parent = null;
    try {
      Method getParent = o.getClass().getMethod("getParent", null);
      parent = getParent.invoke(o, null);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      LOG.log(Level.WARNING, e.getMessage());
    }
    return getTopLevelParent(parent);
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
      if (oldResult != null && !oldResult.equals(newResult)) {
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
      for (Job<?, ?> job : item.getAllJobs()) {
        Run<?, ?> build = job.getLastCompletedBuild();
        if (build != null) {
          Result result = build.getResult();
          if (result.isBetterOrEqualTo(Result.FAILURE))
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
