/*
 * Copyright 2019 Foreseeti AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.foreseeti.mal;

import com.foreseeti.mal.AST.Variable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analyzer {
  private MalLogger LOGGER;
  private AST ast;
  private Map<String, AST.Asset> assets;
  private Map<String, Scope<AST.Association>> fields;
  private Map<String, Scope<AST.AttackStep>> steps;
  private Scope<AST.Variable> variables;
  private Set<String> errors;
  private boolean failed;
  private Map<String, AST.Variable> currentVariables;

  private Analyzer(AST ast, boolean verbose, boolean debug) {
    this.ast = ast;
    LOGGER = new MalLogger("ANALYZER", verbose, debug);
    assets = new LinkedHashMap<>();
    fields = new LinkedHashMap<>();
    steps = new LinkedHashMap<>();
    errors = new HashSet<>();
    currentVariables = new LinkedHashMap<>();
  }

  public static void analyze(AST ast) throws CompilerException {
    analyze(ast, false, false);
  }

  public static void analyze(AST ast, boolean verbose, boolean debug) throws CompilerException {
    new Analyzer(ast, verbose, debug)._analyze();
  }

  private void _analyze() throws CompilerException {
    checkDefines();
    checkCategories();
    checkAssets();
    checkMetas();
    checkExtends(); // might throw

    checkAbstract();
    checkParents(); // might throw

    checkSteps();
    checkFields();
    checkReaches(); // might throw
    // TTC distributions are implementation specific and must be checked in the generated code
  }

  private void checkDefines() {
    Map<String, AST.Define> defines = new HashMap<>();
    for (AST.Define define : ast.getDefines()) {
      AST.Define prevDef = defines.put(define.key.id, define);
      if (prevDef != null) {
        error(
            define,
            String.format(
                "Define '%s' previously defined at %s", define.key.id, prevDef.posString()));
      }
    }
    AST.Define id = defines.get("id");
    if (id != null) {
      if (id.value.isBlank()) {
        error(id, "Define 'id' cannot be empty");
      }
    } else {
      error("Missing required define '#id: \"\"'");
    }
    AST.Define version = defines.get("version");
    if (version != null) {
      if (!version.value.matches("\\d+\\.\\d+\\.\\d+")) {
        error(
            version,
            "Define 'version' must be valid semantic versioning without pre-release identifier and build metadata");
      }
    } else {
      error("Missing required define '#version: \"\"'");
    }
  }

  private void checkCategories() {
    for (AST.Category category : ast.getCategories()) {
      if (category.assets.isEmpty() && category.meta.isEmpty()) {
        LOGGER.warning(
            category.name,
            String.format("Category '%s' contains no assets or metadata", category.name.id));
      }
    }
  }

  private void checkMetas() {
    Map<String, List<AST.Meta>> catMetas = new HashMap<>();
    for (AST.Category category : ast.getCategories()) {
      List<AST.Meta> meta = category.meta;
      if (catMetas.containsKey(category.name.id)) {
        meta.addAll(0, catMetas.get(category.name.id));
      }
      checkMeta(meta);
      catMetas.put(category.name.id, meta);
      for (AST.Asset asset : category.assets) {
        checkMeta(asset.meta);
        for (AST.AttackStep attackStep : asset.attackSteps) {
          checkMeta(attackStep.meta);
        }
      }
    }
    for (AST.Association assoc : ast.getAssociations()) {
      checkMeta(assoc.meta);
    }
  }

  private void checkMeta(List<AST.Meta> lst) {
    Map<AST.MetaType, AST.Meta> metas = new HashMap<>();
    for (AST.Meta meta : lst) {
      if (!metas.containsKey(meta.type)) {
        metas.put(meta.type, meta);
      } else {
        AST.Meta prevDef = metas.get(meta.type);
        error(
            meta,
            String.format("Metadata %s previously defined at %s", meta.type, prevDef.posString()));
      }
    }
  }

  private void checkAssets() {
    for (AST.Category category : ast.getCategories()) {
      for (AST.Asset asset : category.assets) {
        if (assets.containsKey(asset.name.id)) {
          AST.Asset prevDef = assets.get(asset.name.id);
          error(
              asset.name,
              String.format(
                  "Asset '%s' previously defined at %s", asset.name.id, prevDef.name.posString()));
        } else {
          assets.put(asset.name.id, asset);
        }
      }
    }
  }

  private void checkExtends() throws CompilerException {
    boolean err = false;
    for (AST.Asset asset : assets.values()) {
      if (asset.parent.isPresent()) {
        if (getAsset(asset.parent.get()) == null) {
          err = true;
        }
      }
    }
    if (err) {
      throw exception();
    }
  }

  private void checkParents() throws CompilerException {
    boolean err = false;
    for (AST.Asset asset : assets.values()) {
      if (asset.parent.isPresent()) {
        Set<String> parents = new LinkedHashSet<>();
        AST.Asset parent = asset;
        do {
          if (!parents.add(parent.name.id)) {
            StringBuilder sb = new StringBuilder();
            for (String parentName : parents) {
              sb.append(parentName);
              sb.append(" -> ");
            }
            sb.append(parent.name.id);
            error(
                asset.name,
                String.format("Asset '%s' extends in loop '%s'", asset.name.id, sb.toString()));
            err = true;
            break;
          }
          parent = getAsset(parent.parent.get());
        } while (parent.parent.isPresent());
      }
    }
    if (err) {
      throw exception();
    }
  }

  private void checkAbstract() {
    for (AST.Asset parent : assets.values()) {
      if (parent.isAbstract) {
        boolean found = false;
        for (AST.Asset extendee : assets.values()) {
          if (extendee.parent.isPresent() && extendee.parent.get().id.equals(parent.name.id)) {
            found = true;
            break;
          }
        }
        if (!found) {
          LOGGER.warning(
              parent.name,
              String.format("Asset '%s' is abstract but never extended to", parent.name.id));
        }
      }
    }
  }

  private void checkSteps() {
    for (AST.Asset asset : assets.values()) {
      Scope<AST.AttackStep> scope = new Scope<>();
      steps.put(asset.name.id, scope);
      readSteps(scope, asset);
    }
  }

  /**
   * Retrieves a list of an assets parents (including itself). The oldest parents will be first in
   * the list. E.g. Alpha extends Bravo extends Charlie would return [Charlie, Bravo, Alpha] for
   * asset Alpha.
   *
   * @param asset Child asset
   * @return List of parents, oldest parent first in list
   */
  private LinkedList<AST.Asset> getParents(AST.Asset asset) {
    LinkedList<AST.Asset> lst = new LinkedList<>();
    lst.addFirst(asset);
    while (asset.parent.isPresent()) {
      asset = getAsset(asset.parent.get());
      lst.addFirst(asset);
    }
    return lst;
  }

  /**
   * Populates a scope with attack steps of an asset and its parents. Checks semantic rules to make
   * sure scope is correctly filled.
   *
   * @param scope Scope to populate
   * @param asset Child asset
   */
  private void readSteps(Scope<AST.AttackStep> scope, AST.Asset asset) {
    List<AST.Asset> parents = getParents(asset);
    for (AST.Asset parent : parents) {
      scope.enterScope();
      for (AST.AttackStep attackStep : parent.attackSteps) {
        AST.AttackStep prevDef = scope.look(attackStep.name.id);
        if (prevDef == null) {
          // Attack step is not defined in current scope
          prevDef = scope.lookup(attackStep.name.id);
          if (prevDef == null) {
            // Attack step is not defined in any scope
            if (attackStep.reaches.isEmpty() || !attackStep.reaches.get().inherits) {
              // Attack step either doesn't reach anything or reaches with ->, OK
              scope.add(attackStep.name.id, attackStep);
            } else {
              // Attack step reaches something with +> but doesn't exist previously, NOK
              error(
                  attackStep.reaches.get(),
                  String.format(
                      "Cannot inherit attack step '%s' without previous definition",
                      attackStep.name.id));
            }
          } else {
            // Attack step is previously defined in another scope
            if (attackStep.type.equals(prevDef.type)) {
              // Step is of same type as previous, OK
              scope.add(attackStep.name.id, attackStep);
            } else {
              // Step is NOT of same type as previous, NOK
              error(
                  attackStep.name,
                  String.format(
                      "Cannot override attack step '%s' previously defined at %s with different type '%s' =/= '%s'",
                      attackStep.name.id, prevDef.name.posString(), attackStep.type, prevDef.type));
            }
          }
        } else {
          // Attack step is defined in this scope, NOK
          error(
              attackStep.name,
              String.format(
                  "Attack step '%s' previously defined at %s",
                  attackStep.name.id, prevDef.name.posString()));
        }
      }
    }
  }

  private void checkFields() {
    for (AST.Asset asset : assets.values()) {
      Scope<AST.Association> scope = new Scope<>();
      fields.put(asset.name.id, scope);
      readFields(scope, asset);
    }
  }

  /**
   * Populates a scope with field names and associations from an asset and its parents.
   *
   * @param scope Scope to populate
   * @param asset Child asset
   */
  private void readFields(Scope<AST.Association> scope, AST.Asset asset) {
    List<AST.Asset> parents = getParents(asset);
    for (AST.Asset parent : parents) {
      scope.enterScope();
      for (AST.Association assoc : ast.getAssociations()) {
        if (assoc.leftAsset.id.equals(parent.name.id)) {
          addField(scope, asset, assoc.rightField, assoc);
        }
        // Association can be made from one asset to itself
        if (assoc.rightAsset.id.equals(parent.name.id)) {
          addField(scope, asset, assoc.leftField, assoc);
        }
      }
    }
  }

  private void addField(
      Scope<AST.Association> scope, AST.Asset asset, AST.ID field, AST.Association assoc) {
    AST.Association prevDef = scope.lookdown(field.id);
    if (prevDef == null) {
      // Field not previously defined
      AST.ID prevStep = hasStep(asset, field.id);
      if (prevStep == null) {
        scope.add(field.id, assoc);
      } else {
        // Field previously defined as attack step
        error(
            field,
            String.format(
                "Field '%s' previously defined as attack step at %s",
                field.id, prevStep.posString()));
      }
    } else {
      // Field previously defined
      AST.ID prevField;
      if (field.id.equals(prevDef.leftField.id)) {
        prevField = prevDef.leftField;
      } else {
        prevField = prevDef.rightField;
      }
      // prevField and field are switched since we are traveling from child->parent
      error(
          prevField,
          String.format("Field '%s' previously defined at %s", field.id, field.posString()));
    }
  }

  private void addVariable(AST.Variable variable) {
    AST.Variable prevDef = variables.look(variable.name.id);
    if (prevDef == null) {
      variables.add(variable.name.id, variable);
    } else {
      error(
          variable.name,
          String.format(
              "Variable '%s' previously defined at %s",
              variable.name.id, prevDef.name.posString()));
    }
  }

  /** Evaluates each expression reached by an attack step. */
  private void checkReaches() throws CompilerException {
    for (AST.Asset asset : assets.values()) {
      variables = new Scope<>();
      variables.enterScope();

      for (AST.Variable variable : asset.variables) {
        addVariable(variable);
      }

      for (AST.AttackStep attackStep : asset.attackSteps) {
        if (attackStep.type == AST.AttackStepType.EXIST
            || attackStep.type == AST.AttackStepType.NOTEXIST) {
          if (attackStep.requires.isPresent()) {
            // Requires (<-)
            variables.enterScope();
            for (AST.Variable variable : attackStep.requires.get().variables) {
              addVariable(variable);
            }
            for (AST.Expr expr : attackStep.requires.get().requires) {
              // Requires only have expressions that ends in assets/fields, not attack steps.
              checkToAsset(asset, expr);
            }
            variables.exitScope();
          } else {
            error(
                attackStep,
                String.format("Attack step of type '%s' must have require '<-'", attackStep.type));
            continue;
          }
        } else if (attackStep.requires.isPresent()) {
          error(
              attackStep.requires.get(),
              "Require '<-' may only be defined for attack step type exist 'E' or not-exist '!E'");
          continue;
        }

        if (attackStep.reaches.isPresent()) {
          variables.enterScope();
          for (AST.Variable variable : attackStep.reaches.get().variables) {
            addVariable(variable);
          }
          for (AST.Expr expr : attackStep.reaches.get().reaches) {
            checkToStep(asset, expr);
          }
          variables.exitScope();
        }
      }
    }
    if (failed) {
      throw exception();
    }
  }

  private AST.AttackStep variableToStep(AST.Asset asset, AST.Variable variable) {
    if (evalVariableBegin(variable)) {
      AST.AttackStep res = checkToStep(asset, variable.expr);
      evalVariableEnd(variable);
      return res;
    } else {
      return null;
    }
  }

  private AST.AttackStep checkToStep(AST.Asset asset, AST.Expr expr) {
    if (expr instanceof AST.IDExpr) {
      AST.IDExpr step = (AST.IDExpr) expr;
      AST.Asset target = asset;
      AST.Variable variable = variables.lookup(step.id.id);
      AST.AttackStep attackStep = steps.get(target.name.id).lookup(step.id.id);
      if (variable != null && attackStep != null) {
        // ID is both variable and attack step. We assume the variable is desired but print a
        // warning.
        LOGGER.warning(
            step.id,
            String.format(
                "Step '%s' defined as variable at %s and attack step at %s",
                step.id.id, variable.name.posString(), attackStep.name.posString()));
        return variableToStep(target, variable);
      } else if (variable != null) {
        // Only defined as a variable
        return variableToStep(target, variable);
      } else if (attackStep != null) {
        // Only defined as an attack step
        return attackStep;
      } else {
        error(
            step.id,
            String.format(
                "Attack step '%s' not defined for asset '%s'", step.id.id, target.name.id));
        return null;
      }
    } else if (expr instanceof AST.StepExpr) {
      AST.StepExpr step = (AST.StepExpr) expr;
      AST.Asset target = checkToAsset(asset, step.lhs);
      if (target != null) {
        return checkToStep(target, step.rhs);
      } else {
        return null;
      }
    } else {
      error(expr, "Last step is not attack step");
      return null;
    }
  }

  private AST.Asset checkToAsset(AST.Asset asset, AST.Expr expr) {
    if (expr instanceof AST.StepExpr) {
      return checkStepExpr(asset, (AST.StepExpr) expr);
    } else if (expr instanceof AST.IDExpr) {
      return checkIDExpr(asset, (AST.IDExpr) expr);
    } else if (expr instanceof AST.IntersectionExpr || expr instanceof AST.UnionExpr) {
      return checkSetExpr(asset, (AST.BinaryExpr) expr);
    } else if (expr instanceof AST.TransitiveExpr) {
      return checkTransitiveExpr(asset, (AST.TransitiveExpr) expr);
    } else if (expr instanceof AST.SubTypeExpr) {
      return checkSubTypeExpr(asset, (AST.SubTypeExpr) expr);
    } else {
      error(expr, String.format("Unexpected expression '%s'", expr.toString()));
      System.exit(1);
      return null;
    }
  }

  private AST.Asset checkStepExpr(AST.Asset asset, AST.StepExpr expr) {
    AST.Asset leftTarget = checkToAsset(asset, expr.lhs);
    if (leftTarget != null) {
      AST.Asset rightTarget = checkToAsset(leftTarget, expr.rhs);
      return rightTarget;
    } else {
      return null;
    }
  }

  /**
   * When evaluating a variable (variableToAsset() or varialeToStep()), a record must be kept to
   * check for cyclic usage of variables.
   *
   * @param variable Variable evaluated
   * @return True if variable is not being evaluated, false otherwise
   */
  private boolean evalVariableBegin(AST.Variable variable) {
    AST.Variable prev = currentVariables.put(variable.name.id, variable);
    if (prev == null) {
      return true;
    } else {
      StringBuilder sb = new StringBuilder();
      for (String key : currentVariables.keySet()) {
        sb.append(key);
        sb.append(" -> ");
      }
      sb.append(variable.name.id);
      AST.Variable first = (Variable) currentVariables.values().toArray()[0];
      error(
          first.name,
          String.format("Variable '%s' contains cycle '%s'", first.name.id, sb.toString()));
      return false;
    }
  }

  private void evalVariableEnd(AST.Variable variable) {
    currentVariables.remove(variable.name.id);
  }

  private AST.Asset variableToAsset(AST.Asset asset, AST.Variable variable) {
    if (evalVariableBegin(variable)) {
      AST.Asset res = checkToAsset(asset, variable.expr);
      evalVariableEnd(variable);
      return res;
    } else {
      return null;
    }
  }

  private AST.Asset checkIDExpr(AST.Asset asset, AST.IDExpr expr) {
    AST.Variable variable = variables.lookup(expr.id.id);
    AST.ID target = hasTarget(asset, expr.id.id);
    if (variable != null && target != null) {
      // ID found as both variable and field
      LOGGER.warning(
          expr.id,
          String.format(
              "Step '%s' defined as variable at %s and field at %s",
              expr.id.id, variable.name.posString(), target.posString()));
      return variableToAsset(asset, variable);
    } else if (variable != null) {
      // ID defined as variable only
      return variableToAsset(asset, variable);
    } else {
      // ID defined as target (or invalid, getTarget() will print error)
      return getTarget(asset, expr.id);
    }
  }

  private AST.Asset checkSetExpr(AST.Asset asset, AST.BinaryExpr expr) {
    AST.Asset leftTarget = checkToAsset(asset, expr.lhs);
    AST.Asset rightTarget = checkToAsset(asset, expr.rhs);
    if (leftTarget == null || rightTarget == null) {
      return null;
    }
    AST.Asset target = getLCA(leftTarget, rightTarget);
    if (target != null) {
      return target;
    } else {
      error(
          expr,
          String.format(
              "Types '%s' and '%s' have no common ancestor",
              leftTarget.name.id, rightTarget.name.id));
      return null;
    }
  }

  private AST.Asset checkTransitiveExpr(AST.Asset asset, AST.TransitiveExpr expr) {
    AST.Asset res = checkToAsset(asset, expr.e);
    if (isChild(res, asset)) {
      return res;
    } else {
      error(
          expr,
          String.format("Previous asset '%s' is not of type '%s'", asset.name.id, res.name.id));
      return null;
    }
  }

  private AST.Asset checkSubTypeExpr(AST.Asset asset, AST.SubTypeExpr expr) {
    AST.Asset target = checkToAsset(asset, expr.e);
    if (target == null) {
      return null;
    }
    AST.Asset type = getAsset(expr.subType);
    if (type == null) {
      return null;
    }
    if (isChild(target, type)) {
      return type;
    } else {
      error(expr, String.format("Asset '%s' cannot be of type '%s'", target.name.id, type.name.id));
      return null;
    }
  }

  private AST.Asset getAsset(AST.ID name) {
    if (assets.containsKey(name.id)) {
      return assets.get(name.id);
    } else {
      error(name, String.format("Asset '%s' not defined", name.id));
      return null;
    }
  }

  private AST.ID hasStep(AST.Asset asset, String name) {
    Scope<AST.AttackStep> scope = steps.get(asset.name.id);
    AST.AttackStep attackStep = scope.lookdown(name);
    if (attackStep != null) {
      return attackStep.name;
    } else {
      return null;
    }
  }

  private AST.ID hasTarget(AST.Asset asset, String field) {
    Scope<AST.Association> scope = fields.get(asset.name.id);
    AST.Association assoc = scope.lookdown(field);
    if (assoc != null) {
      if (assoc.leftField.id.equals(field)) {
        return assoc.leftField;
      } else {
        return assoc.rightField;
      }
    } else {
      return null;
    }
  }

  private AST.Asset getTarget(AST.Asset asset, AST.ID name) {
    Scope<AST.Association> scope = fields.get(asset.name.id);
    AST.Association assoc = scope.lookdown(name.id);
    if (assoc != null) {
      if (assoc.leftField.id.equals(name.id)) {
        return getAsset(assoc.leftAsset);
      } else {
        return getAsset(assoc.rightAsset);
      }
    } else {
      error(name, String.format("Field '%s' not defined for asset '%s'", name.id, asset.name.id));
      return null;
    }
  }

  private boolean isChild(AST.Asset parent, AST.Asset child) {
    if (parent.name.id.equals(child.name.id)) {
      return true;
    } else if (child.parent.isEmpty()) {
      return false;
    } else {
      AST.Asset childParent = getAsset(child.parent.get());
      return isChild(parent, childParent);
    }
  }

  private AST.Asset getLCA(AST.Asset left, AST.Asset right) {
    if (isChild(left, right)) {
      return left;
    } else if (isChild(right, left)) {
      return right;
    } else if (!left.parent.isPresent() && !right.parent.isPresent()) {
      return null;
    } else {
      AST.Asset lparent = getAsset(left.parent.orElse(left.name));
      AST.Asset rparent = getAsset(right.parent.orElse(right.name));
      return getLCA(lparent, rparent);
    }
  }

  private CompilerException exception() {
    return new CompilerException("There were semantic errors");
  }

  private void error(String msg) {
    failed = true;
    if (errors.add(msg)) {
      LOGGER.error(msg);
    }
  }

  private void error(Position pos, String msg) {
    failed = true;
    if (errors.add(msg)) {
      LOGGER.error(pos, msg);
    }
  }
}
