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
package org.mal_lang.compiler.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AST {
  // <mal>::= (<category> | <associations> | <include> | <define>)* EOF
  private List<Category> categories = new ArrayList<>();
  private List<Association> associations = new ArrayList<>();
  private List<Define> defines = new ArrayList<>();

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append(String.format("%s,%n", Define.listToString(defines, 0)));
    sb.append(String.format("%s,%n", Category.listToString(categories, 0)));
    sb.append(String.format("%s%n", Association.listToString(associations, 0)));
    return sb.toString();
  }

  public void include(AST other) {
    this.categories.addAll(other.categories);
    this.associations.addAll(other.associations);
    this.defines.addAll(other.defines);
  }

  public List<Category> getCategories() {
    var categories = new ArrayList<Category>();
    categories.addAll(this.categories);
    return categories;
  }

  public void addCategory(Category category) {
    this.categories.add(category);
  }

  public List<Association> getAssociations() {
    var associations = new ArrayList<Association>();
    associations.addAll(this.associations);
    return associations;
  }

  public void addAssociations(List<Association> associations) {
    this.associations.addAll(associations);
  }

  public List<Define> getDefines() {
    var defines = new ArrayList<Define>();
    defines.addAll(this.defines);
    return defines;
  }

  public void addDefine(Define define) {
    this.defines.add(define);
  }

  public static class ID extends Position {
    public final String id;

    public ID(Position pos, String id) {
      super(pos);
      this.id = id;
    }

    @Override
    public String toString() {
      return String.format("ID(%s, \"%s\")", posString(), id);
    }
  }

  public static class Define extends Position {
    public final ID key;
    public final String value;

    public Define(Position pos, ID key, String value) {
      super(pos);
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("Define(%s, %s, \"%s\")", posString(), key.toString(), value);
    }

    public static String listToString(List<Define> defines, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sdefines = {%n", indent));
      for (int i = 0; i < defines.size(); i++) {
        sb.append(String.format("%s  %s", indent, defines.get(i).toString()));
        if (i < defines.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public static class Meta extends Position {
    public final ID type;
    public final String string;

    public Meta(Position pos, ID type, String string) {
      super(pos);
      this.type = type;
      this.string = string;
    }

    @Override
    public String toString() {
      return String.format("Meta(%s, %s, \"%s\")", posString(), type.toString(), string);
    }

    public static String listToString(List<Meta> meta, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%smeta = {%n", indent));
      for (int i = 0; i < meta.size(); i++) {
        sb.append(String.format("%s  %s", indent, meta.get(i).toString()));
        if (i < meta.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public static class Category extends Position {
    public final ID name;
    public final List<Meta> meta;
    public final List<Asset> assets;
    public final List<Evidence> evidences;

    public Category(
        Position pos, ID name, List<Meta> meta, List<Asset> assets, List<Evidence>  evidences) {
      super(pos);
      this.name = name;
      this.meta = meta;
      this.assets = assets;
      this.evidences = evidences;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      return String.format("%sCategory(%s, %s,%n", indent, posString(), name.toString()) +
              String.format("%s,%n", Meta.listToString(meta, spaces + 2)) +
              String.format("%s%n", Asset.listToString(assets, spaces + 2)) +
              String.format("%s%n", Evidence.listToString(evidences, spaces + 2)) +
              String.format("%s)", indent);
    }

    public static String listToString(List<Category> categories, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%scategories = {%n", indent));
      for (int i = 0; i < categories.size(); i++) {
        sb.append(String.format("%s", categories.get(i).toString(spaces + 2)));
        if (i < categories.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public static class Asset extends Position {
    public final boolean isAbstract;
    public final ID name;
    public final Optional<ID> parent;
    public final List<Meta> meta;
    public final List<AttackStep> attackSteps;
    public final List<Trace> traces;
    public final List<Variable> variables;

    public Asset(
        Position pos,
        boolean isAbstract,
        ID name,
        Optional<ID> parent,
        List<Meta> meta,
        List<AttackStep> attackSteps,
        List<Trace> traces,
        List<Variable> variables) {
      super(pos);
      this.isAbstract = isAbstract;
      this.name = name;
      this.parent = parent;
      this.meta = meta;
      this.attackSteps = attackSteps;
      this.traces = traces;
      this.variables = variables;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(
          String.format(
              "%sAsset(%s, %s, %s, %s,%n",
              indent,
              posString(),
              isAbstract ? "ABSTRACT" : "NOT_ABSTRACT",
              name.toString(),
              parent.isEmpty()
                  ? "NO_PARENT"
                  : String.format("PARENT(%s)", parent.get().toString())));
      sb.append(String.format("%s,%n", Meta.listToString(meta, spaces + 2)));
      sb.append(String.format("%s,%n", AttackStep.listToString(attackSteps, spaces + 2)));
      sb.append(String.format("%s%n", Variable.listToString(variables, spaces + 2)));
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }

    public static String listToString(List<Asset> assets, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sassets = {%n", indent));
      for (int i = 0; i < assets.size(); i++) {
        sb.append(String.format("%s", assets.get(i).toString(spaces + 2)));
        if (i < assets.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public static class Evidence extends Position {
    public final boolean isAbstract;
    public final ID name;
    public final Optional<ID> parent;
    public final List<Meta> meta;
    public final List<Trace> traces;
    public final List<Variable> variables;

    public Evidence(
        Position pos,
        boolean isAbstract,
        ID name,
        Optional<ID> parent,
        List<Meta> meta,
        List<Trace> traces,
        List<Variable> variables) {
      super(pos);
      this.isAbstract = isAbstract;
      this.name = name;
      this.parent = parent;
      this.meta = meta;
      this.traces = traces;
      this.variables = variables;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      return String.format(
              "%sAsset(%s, %s, %s, %s,%n",
              indent,
              posString(),
              isAbstract ? "ABSTRACT" : "NOT_ABSTRACT",
              name.toString(),
              parent.isEmpty()
                      ? "NO_PARENT"
                      : String.format("PARENT(%s)", parent.get().toString())) +
              String.format("%s,%n", Meta.listToString(meta, spaces + 2)) +
              String.format("%s,%n", Trace.listToString(traces, spaces + 2)) +
              String.format("%s%n", Variable.listToString(variables, spaces + 2)) +
              String.format("%s)", indent);
    }

    public static String listToString(List<Evidence> evidences, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sassets = {%n", indent));
      for (int i = 0; i < evidences.size(); i++) {
        sb.append(String.format("%s", evidences.get(i).toString(spaces + 2)));
        if (i < evidences.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public enum TraceType {
    ALL,
    ANY
  }

  public static class Trace extends Position {
    public final TraceType type;
    public final ID name;
    public final List<ID> tags;
    public final Optional<TTEExpr> tte;
    public final List<Meta> meta;
    public final Optional<Requires> requires;
    public final Optional<Reaches> reaches;

    public Trace(
        Position pos,
        TraceType type,
        ID name,
        List<ID> tags,
        Optional<TTEExpr> tte,
        List<Meta> meta,
        Optional<Requires> requires,
        Optional<Reaches> reaches) {
      super(pos);
      this.type = type;
      this.name = name;
      this.tags = tags;
      this.tte = tte;
      this.meta = meta;
      this.requires = requires;
      this.reaches = reaches;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(
          String.format("%sTrace(%s, %s, %s,%n", indent, posString(), type.name(), name.toString()));
      sb.append(String.format("%s  tags = {", indent));
      for (int i = 0; i < tags.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(tags.get(i).toString());
      }
      sb.append(String.format("},%n"));
      if (tte.isEmpty()) {
        sb.append(String.format("%s  tte = [],%n", indent));
      } else {
        sb.append(String.format("%s  tte = [%s],%n", indent, tte.get().toString()));
      }
      sb.append(String.format("%s,%n", Meta.listToString(meta, spaces + 2)));
      if (requires.isEmpty()) {
        sb.append(String.format("%s  NO_REQUIRES,%n", indent));
      } else {
        sb.append(String.format("%s,%n", requires.get().toString(spaces + 2)));
      }
      if (reaches.isEmpty()) {
        sb.append(String.format("%s  NO_REACHES%n", indent));
      } else {
        sb.append(String.format("%s%n", reaches.get().toString(spaces + 2)));
      }
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }

    public static String listToString(List<Trace> traces, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sattacksteps = {%n", indent));
      for (int i = 0; i < traces.size(); i++) {
        sb.append(String.format("%s", traces.get(i).toString(spaces + 2)));
        if (i < traces.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public enum AttackStepType {
    ALL,
    ANY,
    DEFENSE,
    EXIST,
    NOTEXIST
  }

  public static class AttackStep extends Position {
    public final AttackStepType type;
    public final ID name;
    public final List<ID> tags;
    public final Optional<List<CIA>> cia;
    public final Optional<TTCExpr> ttc;
    public final List<Meta> meta;
    public final Optional<Requires> requires;
    public final Optional<Reaches> reaches;

    public AttackStep(
        Position pos,
        AttackStepType type,
        ID name,
        List<ID> tags,
        Optional<List<CIA>> cia,
        Optional<TTCExpr> ttc,
        List<Meta> meta,
        Optional<Requires> requires,
        Optional<Reaches> reaches) {
      super(pos);
      this.type = type;
      this.name = name;
      this.tags = tags;
      this.cia = cia;
      this.ttc = ttc;
      this.meta = meta;
      this.requires = requires;
      this.reaches = reaches;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(
          String.format(
              "%sAttackStep(%s, %s, %s,%n", indent, posString(), type.name(), name.toString()));
      sb.append(String.format("%s  tags = {", indent));
      for (int i = 0; i < tags.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(tags.get(i).toString());
      }
      sb.append(String.format("},%n"));
      if (cia.isEmpty()) {
        sb.append(String.format("%s  cia = {},%n", indent));
      } else {
        sb.append(String.format("%s  cia = {%s},%n", indent, CIA.listToString(cia.get())));
      }
      if (ttc.isEmpty()) {
        sb.append(String.format("%s  ttc = [],%n", indent));
      } else {
        sb.append(String.format("%s  ttc = [%s],%n", indent, ttc.get().toString()));
      }
      sb.append(String.format("%s,%n", Meta.listToString(meta, spaces + 2)));
      if (requires.isEmpty()) {
        sb.append(String.format("%s  NO_REQUIRES,%n", indent));
      } else {
        sb.append(String.format("%s,%n", requires.get().toString(spaces + 2)));
      }
      if (reaches.isEmpty()) {
        sb.append(String.format("%s  NO_REACHES%n", indent));
      } else {
        sb.append(String.format("%s%n", reaches.get().toString(spaces + 2)));
      }
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }

    public static String listToString(List<AttackStep> attackSteps, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sattacksteps = {%n", indent));
      for (int i = 0; i < attackSteps.size(); i++) {
        sb.append(String.format("%s", attackSteps.get(i).toString(spaces + 2)));
        if (i < attackSteps.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public enum CIA {
    C,
    I,
    A;

    public static String listToString(List<CIA> cia) {
      var sb = new StringBuilder();
      for (int i = 0; i < cia.size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(cia.get(i));
      }
      return sb.toString();
    }
  }

  public abstract static class TTCExpr extends Position {
    public TTCExpr(Position pos) {
      super(pos);
    }
  }

  public abstract static class TTCBinaryExpr extends TTCExpr {
    public final TTCExpr lhs;
    public final TTCExpr rhs;

    public TTCBinaryExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos);
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  public static class TTCAddExpr extends TTCBinaryExpr {
    public TTCAddExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCAddExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTCSubExpr extends TTCBinaryExpr {
    public TTCSubExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCSubExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTCMulExpr extends TTCBinaryExpr {
    public TTCMulExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCMulExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTCDivExpr extends TTCBinaryExpr {
    public TTCDivExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCDivExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTCPowExpr extends TTCBinaryExpr {
    public TTCPowExpr(Position pos, TTCExpr lhs, TTCExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCPowExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTCFuncExpr extends TTCExpr {
    public final ID name;
    public final List<Double> params;

    public TTCFuncExpr(Position pos, ID name, List<Double> params) {
      super(pos);
      this.name = name;
      this.params = params;
    }

    @Override
    public String toString() {
      var sb = new StringBuilder();
      sb.append(String.format("TTCFuncExpr(%s, %s", posString(), name.toString()));
      for (var p : params) {
        sb.append(String.format(", %f", p));
      }
      sb.append(')');
      return sb.toString();
    }
  }

  public static class TTCNumExpr extends TTCExpr {
    public final double value;

    public TTCNumExpr(Position pos, double value) {
      super(pos);
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("TTCNumExpr(%s, %f)", posString(), value);
    }
  }

  public abstract static class TTEExpr extends Position {
    public TTEExpr(Position pos) {
      super(pos);
    }
  }

  public abstract static class TTEBinaryExpr extends TTEExpr {
    public final TTEExpr lhs;
    public final TTEExpr rhs;

    public TTEBinaryExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos);
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  public static class TTEAddExpr extends TTEBinaryExpr {
    public TTEAddExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTEAddExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTESubExpr extends TTEBinaryExpr {
    public TTESubExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTESubExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTEMulExpr extends TTEBinaryExpr {
    public TTEMulExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTCMulExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTEDivExpr extends TTEBinaryExpr {
    public TTEDivExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTEDivExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTEPowExpr extends TTEBinaryExpr {
    public TTEPowExpr(Position pos, TTEExpr lhs, TTEExpr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("TTEPowExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class TTEFuncExpr extends TTEExpr {
    public final ID name;
    public final List<Double> params;

    public TTEFuncExpr(Position pos, ID name, List<Double> params) {
      super(pos);
      this.name = name;
      this.params = params;
    }

    @Override
    public String toString() {
      var sb = new StringBuilder();
      sb.append(String.format("TTEFuncExpr(%s, %s", posString(), name.toString()));
      for (var p : params) {
        sb.append(String.format(", %f", p));
      }
      sb.append(')');
      return sb.toString();
    }
  }

  public static class TTENumExpr extends TTEExpr {
    public final double value;

    public TTENumExpr(Position pos, double value) {
      super(pos);
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("TTENumExpr(%s, %f)", posString(), value);
    }
  }


  public static class Requires extends Position {
    public final List<Expr> requires;

    public Requires(Position pos, List<Expr> requires) {
      super(pos);
      this.requires = requires;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sRequires(%s,%n", indent, posString()));
      sb.append(String.format("%s%n", Expr.listToString(requires, "requires", spaces + 2)));
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }
  }

  public enum ReachTypes {
    INHERIT,
    OVERRIDE,
    LEAVE,
    ERASE,
  }

  public static class Reaches extends Position {
    // public final boolean inherits;
    public final ReachTypes types;
    public final List<Expr> reaches;

    public Reaches(Position pos, ReachTypes types, List<Expr> reaches) {
      super(pos);
      this.types = types;
      // this.inherits = inherits;
      this.reaches = reaches;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      //            sb.append(
      //                    String.format(
      //                            "%sReaches(%s, %s,%n", indent, posString(), inherits ?
      // "INHERITS" : "OVERRIDES"));

      sb.append(String.format("%sReaches(%s, %s,%n", indent, posString(), types));
      sb.append(String.format("%s%n", Expr.listToString(reaches, "reaches", spaces + 2)));
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }
  }

  public static class Variable extends Position {
    public final ID name;
    public final Expr expr;

    public Variable(Position pos, ID name, Expr expr) {
      super(pos);
      this.name = name;
      this.expr = expr;
    }

    @Override
    public String toString() {
      return String.format("Variable(%s, %s, %s)", posString(), name.toString(), expr.toString());
    }

    public static String listToString(List<Variable> variables, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%svariables = {%n", indent));
      for (int i = 0; i < variables.size(); i++) {
        sb.append(String.format("%s  %s", indent, variables.get(i).toString()));
        if (i < variables.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public abstract static class Expr extends Position {
    public Expr(Position pos) {
      super(pos);
    }

    public static String listToString(List<Expr> exprs, String name, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%s%s = {%n", indent, name));
      for (int i = 0; i < exprs.size(); i++) {
        sb.append(String.format("%s  %s", indent, exprs.get(i).toString()));
        if (i < exprs.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public abstract static class BinaryExpr extends Expr {
    public final Expr lhs;
    public final Expr rhs;

    public BinaryExpr(Position pos, Expr lhs, Expr rhs) {
      super(pos);
      this.lhs = lhs;
      this.rhs = rhs;
    }
  }

  public static class UnionExpr extends BinaryExpr {
    public UnionExpr(Position pos, Expr lhs, Expr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("UnionExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class DifferenceExpr extends BinaryExpr {
    public DifferenceExpr(Position pos, Expr lhs, Expr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format(
          "DifferenceExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class IntersectionExpr extends BinaryExpr {
    public IntersectionExpr(Position pos, Expr lhs, Expr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format(
          "IntersectionExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public static class StepExpr extends BinaryExpr {
    public StepExpr(Position pos, Expr lhs, Expr rhs) {
      super(pos, lhs, rhs);
    }

    @Override
    public String toString() {
      return String.format("StepExpr(%s, %s, %s)", posString(), lhs.toString(), rhs.toString());
    }
  }

  public abstract static class UnaryExpr extends Expr {
    public final Expr e;

    public UnaryExpr(Position pos, Expr e) {
      super(pos);
      this.e = e;
    }
  }

  public static class TransitiveExpr extends UnaryExpr {
    public TransitiveExpr(Position pos, Expr e) {
      super(pos, e);
    }

    @Override
    public String toString() {
      return String.format("TransitiveExpr(%s, %s)", posString(), e.toString());
    }
  }

  public static class SubTypeExpr extends UnaryExpr {
    public final ID subType;

    public SubTypeExpr(Position pos, Expr e, ID subType) {
      super(pos, e);
      this.subType = subType;
    }

    @Override
    public String toString() {
      return String.format(
          "SubTypeExpr(%s, %s, %s)", posString(), e.toString(), subType.toString());
    }
  }

  public static class IDExpr extends Expr {
    public final ID id;

    public IDExpr(Position pos, ID id) {
      super(pos);
      this.id = id;
    }

    @Override
    public String toString() {
      return String.format("IDExpr(%s, %s)", posString(), id.toString());
    }
  }

  public static class CallExpr extends Expr {
    public final ID id;

    public CallExpr(Position pos, ID id) {
      super(pos);
      this.id = id;
    }

    @Override
    public String toString() {
      return String.format("CallExpr(%s, %s)", posString(), id.toString());
    }
  }

  public static class Association extends Position {
    public final ID leftAsset;
    public final ID leftField;
    public final Multiplicity leftMult;
    public final ID linkName;
    public final Multiplicity rightMult;
    public final ID rightField;
    public final ID rightAsset;
    public final List<Meta> meta;

    public Association(
        Position pos,
        ID leftAsset,
        ID leftField,
        Multiplicity leftMult,
        ID linkName,
        Multiplicity rightMult,
        ID rightField,
        ID rightAsset,
        List<Meta> meta) {
      super(pos);
      this.leftAsset = leftAsset;
      this.leftField = leftField;
      this.leftMult = leftMult;
      this.linkName = linkName;
      this.rightMult = rightMult;
      this.rightField = rightField;
      this.rightAsset = rightAsset;
      this.meta = meta;
    }

    public String toString(int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(
          String.format(
              "%sAssociation(%s, %s, %s, %s, %s, %s, %s, %s,%n",
              indent,
              posString(),
              leftAsset.toString(),
              leftField.toString(),
              leftMult.name(),
              linkName.toString(),
              rightMult.name(),
              rightField.toString(),
              rightAsset.toString()));
      sb.append(String.format("%s%n", Meta.listToString(meta, spaces + 2)));
      sb.append(String.format("%s)", indent));
      return sb.toString();
    }

    public String toShortString() {
      return String.format(
          "%s [%s] <-- %s --> %s [%s]",
          leftAsset.id, leftField.id, linkName.id, rightAsset.id, rightField.id);
    }

    public static String listToString(List<Association> associations, int spaces) {
      var indent = " ".repeat(spaces);
      var sb = new StringBuilder();
      sb.append(String.format("%sassociations = {%n", indent));
      for (int i = 0; i < associations.size(); i++) {
        sb.append(String.format("%s", associations.get(i).toString(spaces + 2)));
        if (i < associations.size() - 1) {
          sb.append(',');
        }
        sb.append(String.format("%n"));
      }
      sb.append(String.format("%s}", indent));
      return sb.toString();
    }
  }

  public enum Multiplicity {
    ZERO_OR_ONE("0..1"),
    ZERO_OR_MORE("*"),
    ONE("1"),
    ONE_OR_MORE("1..*");

    private String string;

    private Multiplicity(String string) {
      this.string = string;
    }

    @Override
    public String toString() {
      return string;
    }
  }
}
