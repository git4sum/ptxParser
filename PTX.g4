/* PTX 5.0 */

grammar PTX;

program
: modDirectiveList directiveList?
;

modDirectiveList
: modDirective+
;

modDirective
: VERSION VersionNum
| TARGET TargetSpecifier (',' TargetSpecifier)*
| ADDRSIZE Digits
;

VersionNum
: Digits+ '.' Digits+
;

TargetSpecifier
: SM60 | SM61 | SM50 | SM52 | SM53 | SM30 | SM32 | SM35 | SM37 | SM20 | SM10 | SM11 | SM12 | SM13
| TEXTMODEUNIT | TEXTMODEINDEP
| DEBUG
| MAP64TO32
;

directiveList
: (declarationList | directive)+
;

directive
: LinkingDirective? kernelDirective
| LinkingDirective? functionDirective
| controlDirective
| debugDirective
| declarationList
;


controlDirective
: labelName ':' BRANCHTARGETS labelName+ ';'
| labelName ':' CALLTARGETS identifier ';'
| labelName ':' CALLPROTOTYPE ('(' param ')')? '_' paramList? ';'
;

labelName
: identifier
;

performDirective
: MAXNREG Digits | MAXNTID Digits (',' Digits)* | REQNTID Digits (',' Digits)* | MINNCTAPERSM Digits | MAXNCTAPERSM Digits
| PRAGMA '"' identifier '"' ';'
;


PerformanceDirectives
: MAXNREG Digits | MAXNTID Digits (',' Digits)* | REQNTID Digits (',' Digits)* | MINNCTAPERSM Digits | MAXNCTAPERSM Digits
;

debugDirective
: dwarfDebug 
| sectionDebug
| fileDebug
| locDebug
;

dwarfDebug
: DWARF dwarfStringList
;

dwarfStringList
: DwarfStrings
| FOURBYTE identifier
| QUAD identifier
;


DwarfStrings
: BYTE Hexdigits (',' Hexdigits)*
| FOURBYTE Hexdigits (',' Hexdigits)*
| QUAD Hexdigits (',' Hexdigits)*
;

sectionDebug
: SECTION identifier '{' dwarfLine '}'
;

dwarfLine
: dwarftype hexlists
| dwarftype identifier
| dwarftype identifier Unaryop hexlists
;

hexlists
: Hexdigits (',' Hexdigits)*
;

dwarftype
: Bits
;

fileDebug
: Fileindex '"' identifier '"' Fileoption?
;

Fileindex
: FILE Digits
;

Fileoption
: ',' Hexdigits ',' Digits
;

locDebug
: LOC Digits Digits Digits
;

kernelDirective
: ENTRY identifier paramList? performDirective* ('{' statementList '}')?
;

functionDirective
: FUNC ('(' param ')')? identifier paramList? performDirective* ('{' statementList '}')?
;

LinkingDirective
: '.extern' | '.visible' | '.weak'
;

paramList
: '(' param (',' param)* ')'
;

param
: StateSpace type identifier
;

statementList
: declarationList instructionList
;

declarationList
: declaration+
;

declaration
: StateSpace (ALIGN Digits)? type variableInit ';'
| StateSpace TEXREF variableInit ';'
;

instructionList
: instruction+
;

instruction
: '{' instruction+ '}'
| labelName ':' 
| GuardPred? opcode operandList? ';'
;

variableInit
: variable ('=' initialVal)?
;

variable
: regvecVal | arrayVal
;

initialVal
: '{' initialVal '}'
| ',' initialVal
| digitlists
;

digitlists
: Digits (',' Digits)*
;

regvecVal
: identifier RegisterIndex?
;

RegisterIndex
: '<' Digits? '>'
;

arrayVal
: identifier ArrayIndex*
;

ArrayIndex
: '[' Digits? ']'
;

operandList
: operand (',' operand)*
;

operand
: '(' operand ')'
| Unaryop operand
| operand Unaryop operand
| operand Binaryop operand
| operand Opsequence
| registerVariable
| constantExpression
| '[' addressExpressionList ']'
| labelName
| vectorExpression
;

vectorExpression
: '{'? vectorOperand (',' vectorOperand)* '}'?
;

vectorOperand
: IdentifierString VectorOperands?
;

registerVariable
: '%' identifier registernum?
;

registernum
: Digits
;

addressExpressionList
: addressExpression (',' addressExpression)*
;

addressExpression
: registerVariable
| integerLiteral
| identifier
| addressExpression addrOffset
| vectorExpression
;

addrOffset
: Unaryop+ Digits
;

opcode
: identifier oplist*
;

oplist
: Types | StateSpace | Opsequence
;

constantExpression
: constantExpression Binaryop constantExpression
| constantExpression Unaryop constantExpression
| Unaryop constantExpression
| '(' constantExpression ')'
| integerLiteral
| floatLiteral
;

integerLiteral
: Unaryop? decimalLiteral
| Unaryop? OctalLiteral
| Unaryop? HexadecimalLiteral
| Unaryop? BinaryLiteral
;

floatLiteral
: Unaryop? FloatConst
;

HexadecimalLiteral : '0' [xX] Hexdigits+ 'U'? ;
OctalLiteral : '0' [0-7]+ 'U'? ;
BinaryLiteral : '0' [bB] [01]+ 'U'? ;
decimalLiteral : Digits | Decliteral ;

FloatConst: '0' [fFdD] Hexdigits+;

fragment
NONDIGIT : [a-zA-Z_]
;
fragment
DIGIT : [0-9]
;

fragment
Followsym: [a-zA-Z0-9_$]
;

fragment
HEXDIGIT : DIGIT | [a-fA-F]
;

Priop : '(' | ')';
Unaryop : '+' | '-' | '!' | '~';
Binaryop : '*' | '/' | '%'
	  | '+' | '-'  | '>>' | '<<'
	  | '<' | '>' | '<=' | '>='
	  | '==' | '!='
	  | '&' | '^' | '|' | '&&' | '||' ;
//Ternaryop : '?:';

GuardPred
: '@' '!'? IdentifierString
;

StateSpace
: REG | SREG | CONST | GLOBAL | LOCAL | PARAM | SHARED | TEX
;

fragment REG : '.reg' ;
fragment SREG : '.sreg' ;
fragment CONST : '.const' ;
fragment GLOBAL : '.global' ;
fragment LOCAL : '.local' ;
fragment PARAM : '.param' ;
fragment SHARED : '.shared' ;
fragment TEX : '.tex' ;

TEXREF : '.texref' ;

type
: Types
;

Types
: SignedInt | UnsignedInt | FloatingPoint | Bits | Predicate 
;

SignedInt
: SIGN8 | SIGN16 | SIGN32 | SIGN64
| VECTOR2 SIGN8 | VECTOR2 SIGN16 | VECTOR2 SIGN32 | VECTOR2 SIGN64
| VECTOR4 SIGN8 | VECTOR4 SIGN16 | VECTOR4 SIGN32 | VECTOR4 SIGN64
;
UnsignedInt
: UNSIGN8 | UNSIGN16 | UNSIGN32 | UNSIGN64
| VECTOR2 UNSIGN8 | VECTOR2 UNSIGN16 | VECTOR2 UNSIGN32 | VECTOR2 UNSIGN64
| VECTOR4 UNSIGN8 | VECTOR4 UNSIGN16 | VECTOR4 UNSIGN32 | VECTOR4 UNSIGN64
;
FloatingPoint
: FLOAT16 | FLOAT16X | FLOAT32 | FLOAT64
| VECTOR2 FLOAT16 | VECTOR2 FLOAT16X | VECTOR2 FLOAT32 | VECTOR2 FLOAT64
| VECTOR4 FLOAT16 | VECTOR4 FLOAT16X | VECTOR4 FLOAT32 | VECTOR4 FLOAT64
;
Bits
: BITS8 | BITS16 | BITS32 | BITS64
| VECTOR2 BITS8 | VECTOR2 BITS16 | VECTOR2 BITS32 | VECTOR2 BITS64
| VECTOR4 BITS8 | VECTOR4 BITS16 | VECTOR4 BITS32 | VECTOR4 BITS64
;
Predicate
: '.pred'
;

fragment SIGN8 :'.s8' ;
fragment SIGN16 : '.s16' ;
fragment SIGN32 : '.s32' ;
fragment SIGN64 : '.s64' ;
fragment UNSIGN8 :'.u8' ;
fragment UNSIGN16 : '.u16' ;
fragment UNSIGN32 : '.u32' ;
fragment UNSIGN64 : '.u64' ;
fragment FLOAT16 :'.f16' ;
fragment FLOAT16X : '.f16x2' ;
fragment FLOAT32 : '.f32' ;
fragment FLOAT64 : '.f64' ;
fragment BITS8 :'.b8' ;
fragment BITS16 : '.b16' ;
fragment BITS32 : '.b32' ;
fragment BITS64 : '.b64' ;
fragment VECTOR2 : '.v2' ;
fragment VECTOR4 : '.v4' ;


SM60 : 'sm_60' ;
SM61 : 'sm_61' ;
SM50 : 'sm_50' ;
SM52 : 'sm_52' ;
SM53 : 'sm_53' ;
SM30 : 'sm_30' ;
SM32 : 'sm_32' ;
SM35 : 'sm_35' ;
SM37 : 'sm_37' ;
SM20 : 'sm_20' ;
SM10 : 'sm_10' ;
SM11 : 'sm_11' ;
SM12 : 'sm_12' ;
SM13 : 'sm_13' ;
TEXTMODEUNIT : 'texmode_unified' ;
TEXTMODEINDEP : 'texmode_independent' ;
DEBUG : 'debug' ;
MAP64TO32 : 'map_f64_to_f32' ;

VERSION : '.version' ;
TARGET : '.target' ;
ADDRSIZE : '.address_size' ;

ALIGN : '.align' ;

PRAGMA : '.pragma' ;
MAXNREG : '.maxnreg' ;
MAXNTID : '.maxntid' ;
REQNTID : '.reqntid' ;
MINNCTAPERSM : '.minnctapersm' ;
MAXNCTAPERSM : '.maxnctapersm' ;

BRANCHTARGETS : '.branchtargets' ;
CALLTARGETS : '.calltargets' ;
CALLPROTOTYPE : '.callprototype' ;

DWARF : '@@DWARF' ;
BYTE : '.byte' ;
FOURBYTE : '.4byte' ;
QUAD : '.quad' ;

SECTION : '.section' ;
FILE : '.file' ;
LOC : '.loc' ;

ENTRY : '.entry' ;
FUNC : '.func' ;

VectorOperands
: '.x' | '.y' | '.z' | '.w'
| '.r' | '.g' | '.b' | '.a'
;

identifier
: IdentifierString
;

IdentifierString
: NONDIGIT Followsym*
| ('$' | '%') Followsym+
;

Digits
: DIGIT+
;

Decliteral
: [1-9] Digits* 'U'?
;

Hexdigits
: HEXDIGIT+
;

Opsequence
: GeomOpsequence
| '.' IdentifierString
;

GeomOpsequence
: '.1d' | '.2d' | '.3d' | '.2dms'
;

WhiteSpace : [ \t]+ -> skip;
Newline : ( '\r' '\n'? | '\n' ) -> skip;
LineComment : '//' ~[\r\n]* -> skip;
