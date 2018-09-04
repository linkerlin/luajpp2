/*******************************************************************************
* Copyright (c) 2009 Luaj.org. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/

package nl.weeaboo.lua2.compiler;

import static nl.weeaboo.lua2.compiler.LuaC.LUAI_MAXUPVALUES;
import static nl.weeaboo.lua2.compiler.LuaC.LUAI_MAXVARS;
import static nl.weeaboo.lua2.compiler.LuaC.createAbc;
import static nl.weeaboo.lua2.compiler.LuaC.createAbx;
import static nl.weeaboo.lua2.compiler.LuaC.luaAssert;
import static nl.weeaboo.lua2.compiler.LuaC.realloc;
import static nl.weeaboo.lua2.compiler.LuaC.setArgA;
import static nl.weeaboo.lua2.compiler.LuaC.setArgB;
import static nl.weeaboo.lua2.compiler.LuaC.setArgC;
import static nl.weeaboo.lua2.compiler.LuaC.setArgSBx;
import static nl.weeaboo.lua2.vm.Lua.LFIELDS_PER_FLUSH;
import static nl.weeaboo.lua2.vm.Lua.LUA_MULTRET;
import static nl.weeaboo.lua2.vm.Lua.MAXARG_C;
import static nl.weeaboo.lua2.vm.Lua.MAXARG_sBx;
import static nl.weeaboo.lua2.vm.Lua.MAXINDEXRK;
import static nl.weeaboo.lua2.vm.Lua.NO_REG;
import static nl.weeaboo.lua2.vm.Lua.OP_ADD;
import static nl.weeaboo.lua2.vm.Lua.OP_CLOSE;
import static nl.weeaboo.lua2.vm.Lua.OP_CONCAT;
import static nl.weeaboo.lua2.vm.Lua.OP_DIV;
import static nl.weeaboo.lua2.vm.Lua.OP_EQ;
import static nl.weeaboo.lua2.vm.Lua.OP_GETGLOBAL;
import static nl.weeaboo.lua2.vm.Lua.OP_GETTABLE;
import static nl.weeaboo.lua2.vm.Lua.OP_GETUPVAL;
import static nl.weeaboo.lua2.vm.Lua.OP_JMP;
import static nl.weeaboo.lua2.vm.Lua.OP_LE;
import static nl.weeaboo.lua2.vm.Lua.OP_LEN;
import static nl.weeaboo.lua2.vm.Lua.OP_LOADBOOL;
import static nl.weeaboo.lua2.vm.Lua.OP_LOADK;
import static nl.weeaboo.lua2.vm.Lua.OP_LOADNIL;
import static nl.weeaboo.lua2.vm.Lua.OP_LT;
import static nl.weeaboo.lua2.vm.Lua.OP_MOD;
import static nl.weeaboo.lua2.vm.Lua.OP_MOVE;
import static nl.weeaboo.lua2.vm.Lua.OP_MUL;
import static nl.weeaboo.lua2.vm.Lua.OP_NOT;
import static nl.weeaboo.lua2.vm.Lua.OP_POW;
import static nl.weeaboo.lua2.vm.Lua.OP_RETURN;
import static nl.weeaboo.lua2.vm.Lua.OP_SELF;
import static nl.weeaboo.lua2.vm.Lua.OP_SETGLOBAL;
import static nl.weeaboo.lua2.vm.Lua.OP_SETLIST;
import static nl.weeaboo.lua2.vm.Lua.OP_SETTABLE;
import static nl.weeaboo.lua2.vm.Lua.OP_SETUPVAL;
import static nl.weeaboo.lua2.vm.Lua.OP_SUB;
import static nl.weeaboo.lua2.vm.Lua.OP_TEST;
import static nl.weeaboo.lua2.vm.Lua.OP_TESTSET;
import static nl.weeaboo.lua2.vm.Lua.OP_UNM;
import static nl.weeaboo.lua2.vm.Lua.OpArgN;
import static nl.weeaboo.lua2.vm.Lua.getArgA;
import static nl.weeaboo.lua2.vm.Lua.getArgB;
import static nl.weeaboo.lua2.vm.Lua.getArgSBx;
import static nl.weeaboo.lua2.vm.Lua.getBMode;
import static nl.weeaboo.lua2.vm.Lua.getCMode;
import static nl.weeaboo.lua2.vm.Lua.getOpMode;
import static nl.weeaboo.lua2.vm.Lua.getOpcode;
import static nl.weeaboo.lua2.vm.Lua.iABC;
import static nl.weeaboo.lua2.vm.Lua.iABx;
import static nl.weeaboo.lua2.vm.Lua.iAsBx;
import static nl.weeaboo.lua2.vm.Lua.isK;
import static nl.weeaboo.lua2.vm.Lua.rkAsK;
import static nl.weeaboo.lua2.vm.Lua.testTMode;
import static nl.weeaboo.lua2.vm.LuaBoolean.FALSE;
import static nl.weeaboo.lua2.vm.LuaBoolean.TRUE;
import static nl.weeaboo.lua2.vm.LuaConstants.MAXSTACK;
import static nl.weeaboo.lua2.vm.LuaNil.NIL;

import java.util.Map;

import nl.weeaboo.lua2.compiler.LexState.ConsControl;
import nl.weeaboo.lua2.compiler.LexState.ExpDesc;
import nl.weeaboo.lua2.vm.LocVars;
import nl.weeaboo.lua2.vm.Lua;
import nl.weeaboo.lua2.vm.LuaDouble;
import nl.weeaboo.lua2.vm.LuaInteger;
import nl.weeaboo.lua2.vm.LuaString;
import nl.weeaboo.lua2.vm.LuaValue;
import nl.weeaboo.lua2.vm.Prototype;

final class FuncState {

    Prototype f; /* current function header */
    // LTable h; /* table to find (and reuse) elements in `k' */
    Map<LuaValue, Integer> htable; /* table to find (and reuse) elements in `k' */
    FuncState prev; /* enclosing function */
    LexState ls; /* lexical state */
    LuaC luaC; /* compiler being invoked */
    BlockCnt bl; /* chain of current blocks */
    int pc; /* next position to code (equivalent to `ncode') */
    int lasttarget; /* `pc' of last `jump target' */
    IntPtr jpc; /* list of pending jumps to `pc' */
    int freereg; /* first free register */
    int nk; /* number of elements in `k' */
    int np; /* number of elements in `p' */
    short nlocvars; /* number of elements in `locvars' */
    short nactvar; /* number of active local variables */
    UpValueDesc[] upvalues = new UpValueDesc[LUAI_MAXUPVALUES]; /* upvalues */
    short[] actvar = new short[LUAI_MAXVARS]; /* declared-variable stack */

    FuncState() {
    }

    // =============================================================
    // from lcode.h
    // =============================================================

    InstructionPtr getcodePtr(ExpDesc e) {
        return new InstructionPtr(f.code, e.u.s.info);
    }

    int getcode(ExpDesc e) {
        return f.code[e.u.s.info];
    }

    int codeAsBx(int o, int a, int sBx) {
        return codeABx(o, a, sBx + MAXARG_sBx);
    }

    void setmultret(ExpDesc e) {
        setreturns(e, LUA_MULTRET);
    }

    // =============================================================
    // from lparser.c
    // =============================================================

    LocVars getlocvar(int i) {
        return f.locvars[actvar[i]];
    }

    void checklimit(int v, int l, String msg) {
        if (v > l) {
            errorlimit(l, msg);
        }
    }

    void errorlimit(int limit, String what) {
        String msg = (f.linedefined == 0) ? luaC.pushfstring("main function has more than " + limit + " " + what)
                : luaC.pushfstring("function at line " + f.linedefined + " has more than " + limit + " " + what);
        ls.lexerror(msg, 0);
    }

    int indexupvalue(LuaString name, ExpDesc v) {
        int i;
        for (i = 0; i < f.nups; i++) {
            if (upvalues[i].k == v.k && upvalues[i].info == v.u.s.info) {
                luaAssert(name.raweq(f.upvalues[i]));
                return i;
            }
        }
        /* new one */
        checklimit(f.nups + 1, LUAI_MAXUPVALUES, "upvalues");
        if (f.upvalues == null || f.nups + 1 > f.upvalues.length) {
            f.upvalues = realloc(f.upvalues, f.nups * 2 + 1);
        }
        f.upvalues[f.nups] = name;
        luaAssert(v.k == LexState.VLOCAL || v.k == LexState.VUPVAL);
        upvalues[f.nups] = new UpValueDesc();
        upvalues[f.nups].k = (short)(v.k);
        upvalues[f.nups].info = (short)(v.u.s.info);
        return f.nups++;
    }

    int searchvar(LuaString n) {
        int i;
        for (i = nactvar - 1; i >= 0; i--) {
            if (n.raweq(getlocvar(i).varname)) {
                return i;
            }
        }
        return -1; /* not found */
    }

    void markupval(int level) {
        BlockCnt bl = this.bl;
        while (bl != null && bl.nactvar > level) {
            bl = bl.previous;
        }
        if (bl != null) {
            bl.upval = true;
        }
    }

    int singlevaraux(LuaString n, ExpDesc var, int base) {
        int v = searchvar(n); /* look up at current level */
        if (v >= 0) {
            var.init(LexState.VLOCAL, v);
            if (base == 0) {
                markupval(v); /* local will be used as an upval */
            }
            return LexState.VLOCAL;
        } else { /* not found at current level; try upper one */
            if (prev == null) { /* no more levels? */
                /* default is global variable */
                var.init(LexState.VGLOBAL, NO_REG);
                return LexState.VGLOBAL;
            }
            if (prev.singlevaraux(n, var, 0) == LexState.VGLOBAL) {
                return LexState.VGLOBAL;
            }
            var.u.s.info = indexupvalue(n, var); /* else was LOCAL or UPVAL */
            var.k = LexState.VUPVAL; /* upvalue in this level */
            return LexState.VUPVAL;
        }
    }

    void enterblock(BlockCnt bl, boolean isbreakable) {
        bl.breaklist.i = LexState.NO_JUMP;
        bl.isbreakable = isbreakable;
        bl.nactvar = this.nactvar;
        bl.upval = false;
        bl.previous = this.bl;
        this.bl = bl;
        luaAssert(this.freereg == this.nactvar);
    }

    //
    // void leaveblock (FuncState *fs) {
    // BlockCnt *bl = this.bl;
    // this.bl = bl.previous;
    // removevars(this.ls, bl.nactvar);
    // if (bl.upval)
    // this.codeABC(OP_CLOSE, bl.nactvar, 0, 0);
    // /* a block either controls scope or breaks (never both) */
    // assert(!bl.isbreakable || !bl.upval);
    // assert(bl.nactvar == this.nactvar);
    // this.freereg = this.nactvar; /* free registers */
    // this.patchtohere(bl.breaklist);
    // }

    void leaveblock() {
        BlockCnt bl = this.bl;
        this.bl = bl.previous;
        ls.removevars(bl.nactvar);
        if (bl.upval) {
            codeABC(OP_CLOSE, bl.nactvar, 0, 0);
        }
        /* a block either controls scope or breaks (never both) */
        luaAssert(!bl.isbreakable || !bl.upval);
        luaAssert(bl.nactvar == this.nactvar);
        this.freereg = this.nactvar; /* free registers */
        this.patchtohere(bl.breaklist.i);
    }

    void closelistfield(ConsControl cc) {
        if (cc.v.k == LexState.VVOID) {
            return; /* there is no list item */
        }
        this.exp2nextreg(cc.v);
        cc.v.k = LexState.VVOID;
        if (cc.tostore == LFIELDS_PER_FLUSH) {
            this.setlist(cc.t.u.s.info, cc.na, cc.tostore); /* flush */
            cc.tostore = 0; /* no more items pending */
        }
    }

    boolean hasmultret(int k) {
        return ((k) == LexState.VCALL || (k) == LexState.VVARARG);
    }

    void lastlistfield(ConsControl cc) {
        if (cc.tostore == 0) {
            return;
        }
        if (hasmultret(cc.v.k)) {
            setmultret(cc.v);
            setlist(cc.t.u.s.info, cc.na, LUA_MULTRET);
            cc.na--; /** do not count last expression (unknown number of elements) */
        } else {
            if (cc.v.k != LexState.VVOID) {
                exp2nextreg(cc.v);
            }
            this.setlist(cc.t.u.s.info, cc.na, cc.tostore);
        }
    }

    // =============================================================
    // from lcode.c
    // =============================================================

    void nil(int from, int n) {
        InstructionPtr previous;
        if (this.pc > this.lasttarget) { /* no jumps to current position? */
            if (this.pc == 0) { /* function start? */
                if (from >= this.nactvar) {
                    return; /* positions are already clean */
                }
            } else {
                previous = new InstructionPtr(this.f.code, this.pc - 1);
                if (getOpcode(previous.get()) == OP_LOADNIL) {
                    int pfrom = getArgA(previous.get());
                    int pto = getArgB(previous.get());
                    if (pfrom <= from && from <= pto + 1) { /* can connect both? */
                        if (from + n - 1 > pto) {
                            setArgB(previous, from + n - 1);
                        }
                        return;
                    }
                }
            }
        }
        /* else no optimization */
        this.codeABC(OP_LOADNIL, from, from + n - 1, 0);
    }

    int jump() {
        int jpc = this.jpc.i; /* save list of jumps to here */
        this.jpc.i = LexState.NO_JUMP;
        IntPtr j = new IntPtr(this.codeAsBx(OP_JMP, 0, LexState.NO_JUMP));
        this.concat(j, jpc); /* keep them on hold */
        return j.i;
    }

    void ret(int first, int nret) {
        this.codeABC(OP_RETURN, first, nret + 1, 0);
    }

    int condjump(int /* OpCode */ op, int a, int b, int c) {
        this.codeABC(op, a, b, c);
        return this.jump();
    }

    void fixjump(int pc, int dest) {
        InstructionPtr jmp = new InstructionPtr(this.f.code, pc);
        int offset = dest - (pc + 1);
        luaAssert(dest != LexState.NO_JUMP);
        if (Math.abs(offset) > MAXARG_sBx) {
            ls.syntaxerror("control structure too long");
        }
        setArgSBx(jmp, offset);
    }

    /*
     * * returns current `pc' and marks it as a jump target (to avoid wrong * optimizations with consecutive
     * instructions not in the same basic block).
     */
    int getlabel() {
        this.lasttarget = this.pc;
        return this.pc;
    }

    int getjump(int pc) {
        int offset = getArgSBx(this.f.code[pc]);
        /* point to itself represents end of list */
        if (offset == LexState.NO_JUMP) {
            /* end of list */
            return LexState.NO_JUMP;
        } else {
            /* turn offset into absolute position */
            return (pc + 1) + offset;
        }
    }

    InstructionPtr getjumpcontrol(int pc) {
        InstructionPtr pi = new InstructionPtr(this.f.code, pc);
        if (pc >= 1 && testTMode(getOpcode(pi.code[pi.idx - 1]))) {
            return new InstructionPtr(pi.code, pi.idx - 1);
        } else {
            return pi;
        }
    }

    /*
     * * check whether list has any jump that do not produce a value * (or produce an inverted value)
     */
    boolean need_value(int list) {
        for (; list != LexState.NO_JUMP; list = this.getjump(list)) {
            int i = this.getjumpcontrol(list).get();
            if (getOpcode(i) != OP_TESTSET) {
                return true;
            }
        }
        return false; /* not found */
    }

    boolean patchtestreg(int node, int reg) {
        InstructionPtr i = this.getjumpcontrol(node);
        if (getOpcode(i.get()) != OP_TESTSET) {
            /* cannot patch other instructions */
            return false;
        }
        if (reg != NO_REG && reg != getArgB(i.get())) {
            setArgA(i, reg);
        } else {
            /* no register to put value or register already has the value */
            i.set(createAbc(OP_TEST, getArgB(i.get()), 0, Lua.getArgC(i.get())));
        }
        return true;
    }

    void removevalues(int list) {
        for (; list != LexState.NO_JUMP; list = getjump(list)) {
            patchtestreg(list, NO_REG);
        }
    }

    void patchlistaux(int list, int vtarget, int reg, int dtarget) {
        while (list != LexState.NO_JUMP) {
            int next = getjump(list);
            if (patchtestreg(list, reg)) {
                fixjump(list, vtarget);
            } else {
                fixjump(list, dtarget); /* jump to default target */
            }
            list = next;
        }
    }

    void dischargejpc() {
        patchlistaux(this.jpc.i, this.pc, NO_REG, this.pc);
        this.jpc.i = LexState.NO_JUMP;
    }

    void patchlist(int list, int target) {
        if (target == this.pc) {
            patchtohere(list);
        } else {
            luaAssert(target < this.pc);
            patchlistaux(list, target, NO_REG, target);
        }
    }

    void patchtohere(int list) {
        getlabel();
        concat(this.jpc, list);
    }

    void concat(IntPtr l1, int l2) {
        if (l2 == LexState.NO_JUMP) {
            return;
        }
        if (l1.i == LexState.NO_JUMP) {
            l1.i = l2;
        } else {
            int list = l1.i;
            int next;
            while ((next = getjump(list)) != LexState.NO_JUMP) {
                /* find last element */
                list = next;
            }
            fixjump(list, l2);
        }
    }

    void checkstack(int n) {
        int newstack = this.freereg + n;
        if (newstack > this.f.maxstacksize) {
            if (newstack >= MAXSTACK) {
                ls.syntaxerror("function or expression too complex");
            }
            this.f.maxstacksize = newstack;
        }
    }

    void reserveregs(int n) {
        checkstack(n);
        this.freereg += n;
    }

    void freereg(int reg) {
        if (!isK(reg) && reg >= this.nactvar) {
            this.freereg--;
            luaAssert(reg == this.freereg);
        }
    }

    void freeexp(ExpDesc e) {
        if (e.k == LexState.VNONRELOC) {
            freereg(e.u.s.info);
        }
    }

    int addk(LuaValue v) {
        int idx;
        if (this.htable.containsKey(v)) {
            idx = htable.get(v);
        } else {
            idx = this.nk;
            this.htable.put(v, Integer.valueOf(idx));
            final Prototype f = this.f;
            if (f.k == null || nk + 1 >= f.k.length) {
                f.k = realloc(f.k, nk * 2 + 1);
            }
            f.k[this.nk++] = v;
        }
        return idx;
    }

    int stringK(LuaString s) {
        return this.addk(s);
    }

    int numberK(LuaValue r) {
        if (r instanceof LuaDouble) {
            double d = r.todouble();
            int i = (int)d;
            if (d == i) {
                r = LuaInteger.valueOf(i);
            }
        }
        return this.addk(r);
    }

    int boolK(boolean b) {
        return this.addk(b ? TRUE : FALSE);
    }

    int nilK() {
        return this.addk(NIL);
    }

    void setreturns(ExpDesc e, int nresults) {
        if (e.k == LexState.VCALL) { /* expression is an open function call? */
            setArgC(this.getcodePtr(e), nresults + 1);
        } else if (e.k == LexState.VVARARG) {
            setArgB(this.getcodePtr(e), nresults + 1);
            setArgA(this.getcodePtr(e), this.freereg);
            this.reserveregs(1);
        }
    }

    void setoneret(ExpDesc e) {
        if (e.k == LexState.VCALL) { /* expression is an open function call? */
            e.k = LexState.VNONRELOC;
            e.u.s.info = getArgA(this.getcode(e));
        } else if (e.k == LexState.VVARARG) {
            setArgB(this.getcodePtr(e), 2);
            e.k = LexState.VRELOCABLE; /* can relocate its simple result */
        }
    }

    void dischargevars(ExpDesc e) {
        switch (e.k) {
        case LexState.VLOCAL: {
            e.k = LexState.VNONRELOC;
            break;
        }
        case LexState.VUPVAL: {
            e.u.s.info = this.codeABC(OP_GETUPVAL, 0, e.u.s.info, 0);
            e.k = LexState.VRELOCABLE;
            break;
        }
        case LexState.VGLOBAL: {
            e.u.s.info = this.codeABx(OP_GETGLOBAL, 0, e.u.s.info);
            e.k = LexState.VRELOCABLE;
            break;
        }
        case LexState.VINDEXED: {
            this.freereg(e.u.s.aux);
            this.freereg(e.u.s.info);
            e.u.s.info = this.codeABC(OP_GETTABLE, 0, e.u.s.info, e.u.s.aux);
            e.k = LexState.VRELOCABLE;
            break;
        }
        case LexState.VVARARG:
        case LexState.VCALL: {
            this.setoneret(e);
            break;
        }
        default:
            break; /* there is one value available (somewhere) */
        }
    }

    int code_label(int a, int b, int jump) {
        this.getlabel(); /* those instructions may be jump targets */
        return this.codeABC(OP_LOADBOOL, a, b, jump);
    }

    void discharge2reg(ExpDesc e, int reg) {
        this.dischargevars(e);
        switch (e.k) {
        case LexState.VNIL: {
            this.nil(reg, 1);
            break;
        }
        case LexState.VFALSE:
        case LexState.VTRUE: {
            this.codeABC(OP_LOADBOOL, reg, (e.k == LexState.VTRUE ? 1 : 0), 0);
            break;
        }
        case LexState.VK: {
            this.codeABx(OP_LOADK, reg, e.u.s.info);
            break;
        }
        case LexState.VKNUM: {
            this.codeABx(OP_LOADK, reg, this.numberK(e.u.nval()));
            break;
        }
        case LexState.VRELOCABLE: {
            InstructionPtr pc = this.getcodePtr(e);
            setArgA(pc, reg);
            break;
        }
        case LexState.VNONRELOC: {
            if (reg != e.u.s.info) {
                codeABC(OP_MOVE, reg, e.u.s.info, 0);
            }
            break;
        }
        default: {
            luaAssert(e.k == LexState.VVOID || e.k == LexState.VJMP);
            return; /* nothing to do... */
        }
        }
        e.u.s.info = reg;
        e.k = LexState.VNONRELOC;
    }

    void discharge2anyreg(ExpDesc e) {
        if (e.k != LexState.VNONRELOC) {
            this.reserveregs(1);
            this.discharge2reg(e, this.freereg - 1);
        }
    }

    void exp2reg(ExpDesc e, int reg) {
        this.discharge2reg(e, reg);
        if (e.k == LexState.VJMP) {
            concat(e.t, e.u.s.info); /* put this jump in `t' list */
        }
        if (e.hasjumps()) {
            int posFalse = LexState.NO_JUMP; /* position of an eventual LOAD false */
            int posTrue = LexState.NO_JUMP; /* position of an eventual LOAD true */
            if (need_value(e.t.i) || need_value(e.f.i)) {
                int fj = (e.k == LexState.VJMP) ? LexState.NO_JUMP : jump();
                posFalse = code_label(reg, 0, 1);
                posTrue = code_label(reg, 1, 0);
                patchtohere(fj);
            }

            /* position after whole expression */
            int finalPos = this.getlabel();

            patchlistaux(e.f.i, finalPos, reg, posFalse);
            patchlistaux(e.t.i, finalPos, reg, posTrue);
        }
        e.f.i = e.t.i = LexState.NO_JUMP;
        e.u.s.info = reg;
        e.k = LexState.VNONRELOC;
    }

    void exp2nextreg(ExpDesc e) {
        this.dischargevars(e);
        this.freeexp(e);
        this.reserveregs(1);
        this.exp2reg(e, this.freereg - 1);
    }

    int exp2anyreg(ExpDesc e) {
        this.dischargevars(e);
        if (e.k == LexState.VNONRELOC) {
            if (!e.hasjumps()) {
                return e.u.s.info; /* exp is already in a register */
            }
            if (e.u.s.info >= this.nactvar) { /* reg. is not a local? */
                this.exp2reg(e, e.u.s.info); /* put value on it */
                return e.u.s.info;
            }
        }
        this.exp2nextreg(e); /* default */
        return e.u.s.info;
    }

    void exp2val(ExpDesc e) {
        if (e.hasjumps()) {
            exp2anyreg(e);
        } else {
            dischargevars(e);
        }
    }

    int exp2RK(ExpDesc e) {
        this.exp2val(e);
        switch (e.k) {
        case LexState.VKNUM:
        case LexState.VTRUE:
        case LexState.VFALSE:
        case LexState.VNIL: {
            if (this.nk <= MAXINDEXRK) { /* constant fit in RK operand? */
                e.u.s.info = (e.k == LexState.VNIL) ? this.nilK()
                        : (e.k == LexState.VKNUM) ? this.numberK(e.u.nval())
                                : this.boolK((e.k == LexState.VTRUE));
                e.k = LexState.VK;
                return rkAsK(e.u.s.info);
            } else {
                break;
            }
        }
        case LexState.VK: {
            if (e.u.s.info <= MAXINDEXRK) { /* constant fit in argC? */
                return rkAsK(e.u.s.info);
            } else {
                break;
            }
        }
        default:
            break;
        }
        /* not a constant in the right range: put it in a register */
        return this.exp2anyreg(e);
    }

    void storevar(ExpDesc var, ExpDesc ex) {
        switch (var.k) {
        case LexState.VLOCAL: {
            this.freeexp(ex);
            this.exp2reg(ex, var.u.s.info);
            return;
        }
        case LexState.VUPVAL: {
            int e = this.exp2anyreg(ex);
            this.codeABC(OP_SETUPVAL, e, var.u.s.info, 0);
            break;
        }
        case LexState.VGLOBAL: {
            int e = this.exp2anyreg(ex);
            this.codeABx(OP_SETGLOBAL, e, var.u.s.info);
            break;
        }
        case LexState.VINDEXED: {
            int e = this.exp2RK(ex);
            this.codeABC(OP_SETTABLE, var.u.s.info, var.u.s.aux, e);
            break;
        }
        default: {
            luaAssert(false); /* invalid var kind to store */
            break;
        }
        }
        this.freeexp(ex);
    }

    void self(ExpDesc e, ExpDesc key) {
        int func;
        this.exp2anyreg(e);
        this.freeexp(e);
        func = this.freereg;
        this.reserveregs(2);
        this.codeABC(OP_SELF, func, e.u.s.info, this.exp2RK(key));
        this.freeexp(key);
        e.u.s.info = func;
        e.k = LexState.VNONRELOC;
    }

    void invertjump(ExpDesc e) {
        InstructionPtr pc = this.getjumpcontrol(e.u.s.info);
        luaAssert(testTMode(getOpcode(pc.get())) && getOpcode(pc.get()) != OP_TESTSET
                && Lua.getOpcode(pc.get()) != OP_TEST);
        // SETARG_A(pc, !(GETARG_A(pc.get())));
        int a = getArgA(pc.get());
        int nota = (a != 0 ? 0 : 1);
        setArgA(pc, nota);
    }

    int jumponcond(ExpDesc e, int cond) {
        if (e.k == LexState.VRELOCABLE) {
            int ie = this.getcode(e);
            if (getOpcode(ie) == OP_NOT) {
                this.pc--; /* remove previous OP_NOT */
                return this.condjump(OP_TEST, getArgB(ie), 0, (cond != 0 ? 0 : 1));
            }
            /* else go through */
        }
        this.discharge2anyreg(e);
        this.freeexp(e);
        return this.condjump(OP_TESTSET, NO_REG, e.u.s.info, cond);
    }

    void goiftrue(ExpDesc e) {
        int pc; /* pc of last jump */
        this.dischargevars(e);
        switch (e.k) {
        case LexState.VK:
        case LexState.VKNUM:
        case LexState.VTRUE: {
            pc = LexState.NO_JUMP; /* always true; do nothing */
            break;
        }
        case LexState.VFALSE: {
            pc = this.jump(); /* always jump */
            break;
        }
        case LexState.VJMP: {
            this.invertjump(e);
            pc = e.u.s.info;
            break;
        }
        default: {
            pc = this.jumponcond(e, 0);
            break;
        }
        }
        this.concat(e.f, pc); /* insert last jump in `f' list */
        this.patchtohere(e.t.i);
        e.t.i = LexState.NO_JUMP;
    }

    void goiffalse(ExpDesc e) {
        int pc; /* pc of last jump */
        this.dischargevars(e);
        switch (e.k) {
        case LexState.VNIL:
        case LexState.VFALSE: {
            pc = LexState.NO_JUMP; /* always false; do nothing */
            break;
        }
        case LexState.VTRUE: {
            pc = this.jump(); /* always jump */
            break;
        }
        case LexState.VJMP: {
            pc = e.u.s.info;
            break;
        }
        default: {
            pc = this.jumponcond(e, 1);
            break;
        }
        }
        this.concat(e.t, pc); /* insert last jump in `t' list */
        this.patchtohere(e.f.i);
        e.f.i = LexState.NO_JUMP;
    }

    void codenot(ExpDesc e) {
        this.dischargevars(e);
        switch (e.k) {
        case LexState.VNIL:
        case LexState.VFALSE: {
            e.k = LexState.VTRUE;
            break;
        }
        case LexState.VK:
        case LexState.VKNUM:
        case LexState.VTRUE: {
            e.k = LexState.VFALSE;
            break;
        }
        case LexState.VJMP: {
            this.invertjump(e);
            break;
        }
        case LexState.VRELOCABLE:
        case LexState.VNONRELOC: {
            this.discharge2anyreg(e);
            this.freeexp(e);
            e.u.s.info = this.codeABC(OP_NOT, 0, e.u.s.info, 0);
            e.k = LexState.VRELOCABLE;
            break;
        }
        default: {
            luaAssert(false); /* cannot happen */
            break;
        }
        }
        /* interchange true and false lists */
        {
            int temp = e.f.i;
            e.f.i = e.t.i;
            e.t.i = temp;
        }
        this.removevalues(e.f.i);
        this.removevalues(e.t.i);
    }

    void indexed(ExpDesc t, ExpDesc k) {
        t.u.s.aux = this.exp2RK(k);
        t.k = LexState.VINDEXED;
    }

    boolean constfolding(int op, ExpDesc e1, ExpDesc e2) {
        if (!e1.isnumeral() || !e2.isnumeral()) {
            return false;
        }

        LuaValue v1 = e1.u.nval();
        LuaValue v2 = e2.u.nval();
        LuaValue r;
        switch (op) {
        case OP_ADD:
            r = v1.add(v2);
            break;
        case OP_SUB:
            r = v1.sub(v2);
            break;
        case OP_MUL:
            r = v1.mul(v2);
            break;
        case OP_DIV:
            r = v1.div(v2);
            break;
        case OP_MOD:
            r = v1.mod(v2);
            break;
        case OP_POW:
            r = v1.pow(v2);
            break;
        case OP_UNM:
            r = v1.neg();
            break;
        case OP_LEN:
            // r = v1.len();
            // break;
            return false; /* no constant folding for 'len' */
        default:
            luaAssert(false);
            r = null;
            return false;
        }
        if (Double.isNaN(r.todouble())) {
            return false; /* do not attempt to produce NaN */
        }
        e1.u.setNval(r);
        return true;
    }

    void codearith(int op, ExpDesc e1, ExpDesc e2) {
        if (constfolding(op, e1, e2)) {
            return;
        } else {
            int o2 = (op != OP_UNM && op != OP_LEN) ? this.exp2RK(e2) : 0;
            int o1 = exp2RK(e1);
            if (o1 > o2) {
                freeexp(e1);
                freeexp(e2);
            } else {
                freeexp(e2);
                freeexp(e1);
            }
            e1.u.s.info = codeABC(op, 0, o1, o2);
            e1.k = LexState.VRELOCABLE;
        }
    }

    void codecomp(int /* OpCode */ op, int cond, ExpDesc e1, ExpDesc e2) {
        int o1 = this.exp2RK(e1);
        int o2 = this.exp2RK(e2);
        this.freeexp(e2);
        this.freeexp(e1);
        if (cond == 0 && op != OP_EQ) {
            int temp; /* exchange args to replace by `<' or `<=' */
            temp = o1;
            o1 = o2;
            o2 = temp; /* o1 <==> o2 */
            cond = 1;
        }
        e1.u.s.info = this.condjump(op, cond, o1, o2);
        e1.k = LexState.VJMP;
    }

    void prefix(int /* UnOpr */ op, ExpDesc e) {
        ExpDesc e2 = new ExpDesc();
        e2.init(LexState.VKNUM, 0);
        switch (op) {
        case LexState.OPR_MINUS: {
            if (e.k == LexState.VK) {
                exp2anyreg(e); /* cannot operate on non-numeric constants */
            }
            codearith(OP_UNM, e, e2);
            break;
        }
        case LexState.OPR_NOT:
            codenot(e);
            break;
        case LexState.OPR_LEN: {
            exp2anyreg(e); /* cannot operate on constants */
            codearith(OP_LEN, e, e2);
            break;
        }
        default:
            luaAssert(false);
        }
    }

    void infix(int /* BinOpr */ op, ExpDesc v) {
        switch (op) {
        case LexState.OPR_AND: {
            this.goiftrue(v);
            break;
        }
        case LexState.OPR_OR: {
            this.goiffalse(v);
            break;
        }
        case LexState.OPR_CONCAT: {
            this.exp2nextreg(v); /* operand must be on the `stack' */
            break;
        }
        case LexState.OPR_ADD:
        case LexState.OPR_SUB:
        case LexState.OPR_MUL:
        case LexState.OPR_DIV:
        case LexState.OPR_MOD:
        case LexState.OPR_POW: {
            if (!v.isnumeral()) {
                exp2RK(v);
            }
            break;
        }
        default: {
            this.exp2RK(v);
            break;
        }
        }
    }

    void posfix(int op, ExpDesc e1, ExpDesc e2) {
        switch (op) {
        case LexState.OPR_AND: {
            luaAssert(e1.t.i == LexState.NO_JUMP); /* list must be closed */
            this.dischargevars(e2);
            this.concat(e2.f, e1.f.i);
            // *e1 = *e2;
            e1.setvalue(e2);
            break;
        }
        case LexState.OPR_OR: {
            luaAssert(e1.f.i == LexState.NO_JUMP); /* list must be closed */
            this.dischargevars(e2);
            this.concat(e2.t, e1.t.i);
            // *e1 = *e2;
            e1.setvalue(e2);
            break;
        }
        case LexState.OPR_CONCAT: {
            this.exp2val(e2);
            if (e2.k == LexState.VRELOCABLE && getOpcode(this.getcode(e2)) == OP_CONCAT) {
                luaAssert(e1.u.s.info == getArgB(this.getcode(e2)) - 1);
                this.freeexp(e1);
                setArgB(this.getcodePtr(e2), e1.u.s.info);
                e1.k = LexState.VRELOCABLE;
                e1.u.s.info = e2.u.s.info;
            } else {
                this.exp2nextreg(e2); /* operand must be on the 'stack' */
                this.codearith(OP_CONCAT, e1, e2);
            }
            break;
        }
        case LexState.OPR_ADD:
            this.codearith(OP_ADD, e1, e2);
            break;
        case LexState.OPR_SUB:
            this.codearith(OP_SUB, e1, e2);
            break;
        case LexState.OPR_MUL:
            this.codearith(OP_MUL, e1, e2);
            break;
        case LexState.OPR_DIV:
            this.codearith(OP_DIV, e1, e2);
            break;
        case LexState.OPR_MOD:
            this.codearith(OP_MOD, e1, e2);
            break;
        case LexState.OPR_POW:
            this.codearith(OP_POW, e1, e2);
            break;
        case LexState.OPR_EQ:
            this.codecomp(OP_EQ, 1, e1, e2);
            break;
        case LexState.OPR_NE:
            this.codecomp(OP_EQ, 0, e1, e2);
            break;
        case LexState.OPR_LT:
            this.codecomp(OP_LT, 1, e1, e2);
            break;
        case LexState.OPR_LE:
            this.codecomp(OP_LE, 1, e1, e2);
            break;
        case LexState.OPR_GT:
            this.codecomp(OP_LT, 0, e1, e2);
            break;
        case LexState.OPR_GE:
            this.codecomp(OP_LE, 0, e1, e2);
            break;
        default:
            luaAssert(false);
        }
    }

    void fixline(int line) {
        this.f.lineinfo[this.pc - 1] = line;
    }

    int code(int instruction, int line) {
        Prototype f = this.f;
        this.dischargejpc(); /* `pc' will change */
        /* put new instruction in code array */
        if (f.code == null || this.pc + 1 > f.code.length) {
            f.code = LuaC.realloc(f.code, this.pc * 2 + 1);
        }
        f.code[this.pc] = instruction;
        /* save corresponding line information */
        if (f.lineinfo == null || this.pc + 1 > f.lineinfo.length) {
            f.lineinfo = LuaC.realloc(f.lineinfo, this.pc * 2 + 1);
        }
        f.lineinfo[this.pc] = line;
        return this.pc++;
    }

    int codeABC(int o, int a, int b, int c) {
        luaAssert(getOpMode(o) == iABC);
        luaAssert(getBMode(o) != OpArgN || b == 0);
        luaAssert(getCMode(o) != OpArgN || c == 0);
        return this.code(createAbc(o, a, b, c), this.ls.lastline);
    }

    int codeABx(int o, int a, int bc) {
        luaAssert(getOpMode(o) == iABx || getOpMode(o) == iAsBx);
        luaAssert(getCMode(o) == OpArgN);
        return this.code(createAbx(o, a, bc), this.ls.lastline);
    }

    void setlist(int base, int nelems, int tostore) {
        int c = (nelems - 1) / LFIELDS_PER_FLUSH + 1;
        int b = (tostore == LUA_MULTRET) ? 0 : tostore;
        luaAssert(tostore != 0);
        if (c <= MAXARG_C) {
            codeABC(OP_SETLIST, base, b, c);
        } else {
            codeABC(OP_SETLIST, base, b, 0);
            code(c, this.ls.lastline);
        }
        this.freereg = base + 1; /* free registers with list values */
    }

}
