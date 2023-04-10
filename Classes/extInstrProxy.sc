// if InstrProxy not installed you will get a warning: Class extension for nonexistent class 'InstrProxy'
+ InstrProxy {

    +> {|str|
        var synthdefmodule = this.getSynthDefModule;
        synthdefmodule.modules.clear;
        synthdefmodule.parse(str)
    }

    getSynthDefModule {
        var synthdefmodule;
        synthdefmodule = Library.at(this, \synthdefmodule);
        if (synthdefmodule.isNil) {
            var me = this;
            var key = me.key;
            synthdefmodule = SynthDefModule();
            synthdefmodule.addDependant({|obj, what, vals|
                //[obj, what, vals].postln;
                fork {
                    await {|done|
                        obj.add(key);
                        Server.default.sync;
                        done.value(\ok);
                    };
                    // re-initialize the synth
                    msgFunc = SynthDescLib.global[key].msgFunc;
                    me.instrument = key;
                    me.metadata.putAll(obj.metadata);
                };
            });
            this.addDependant({|obj, what|
                if (what == \clear) {
                    "clear synthdefmodule".debug(key);
                    Library.put(this, \synthdefmodule, nil)
                }
            });
            Library.put(this, \synthdefmodule, synthdefmodule);
        };
        ^synthdefmodule;
    }

    view {|cmds|
        // TODO: using topenvironment as a sort of cache
        // but probably can use Halo instead
        ^UiModule('sgui').envir_(topEnvironment).view(this, nil, cmds);
    }

    gui {|cmds|
        this.view(cmds).front
    }
}
