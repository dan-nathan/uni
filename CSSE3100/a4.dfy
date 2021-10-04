datatype Category = Whitespace | Identifier
                  | Number | Operator
                  | Error | End

predicate method Is(ch: char, cat: Category)
    requires cat != End
    decreases cat == Error
{
    match cat
    case Whitespace => ch in " \t\r\n"
    case Identifier => 'A' <= ch <= 'Z' || 'a' <= ch <= 'z'
    case Number => '0' <= ch <= '9'
    case Operator => ch in "+-*/%!=><~^&|"
    case Error =>
        !Is(ch, Whitespace) && !Is(ch, Identifier) &&
        !Is(ch, Number) && !Is(ch, Operator)
}

class Tokenizer {
    ghost const source: string
    ghost var n: nat
    var suffix: string
    ghost var m: nat
    var j: nat
    predicate Valid()
        reads this
    {
        n <= |source| 
        && n == m + j
        && suffix == source[m..]
    }

    constructor (s: string)
        ensures Valid() && source == s && m == 0 && j == 0
    {
        source := s;
        suffix := s;
        m := 0;
        j := 0;
        n := 0;
    }

    method Read() returns (cat: Category, ghost p: nat, token: string)
        requires Valid()
        modifies this
        ensures Valid()
        ensures cat != Whitespace
        ensures old(j) <= p <= j <= |suffix|
        ensures cat == End <==> p == |suffix|
        ensures cat == End || cat == Error <==> p == j
        ensures forall i :: old(j) <= i < p ==> Is(suffix[i], Whitespace)
        ensures forall i :: p <= i < j ==> Is(suffix[i], cat)
        ensures p < j ==> j == |suffix| || !Is(suffix[j], cat)
        ensures token == suffix[p..j]
    {
        // skip whitespace
        while j != |suffix| && Is(suffix[j], Whitespace)
            decreases if j <= |suffix| then |suffix| - j else j - |suffix|
            invariant suffix == old(suffix)
            invariant m == old(m)
            invariant n == m + j
            invariant old(j) <= j <= |suffix|
            invariant forall i :: old(j) <= i < j ==> Is(suffix[i], Whitespace)
        {
            j := j + 1;
            n := n + 1;
        }
        p := j;

        // determine syntactic category
        if j == |suffix| {
            return End, p, "";
        } else if Is(suffix[j], Identifier) {
            cat := Identifier;
        } else if Is(suffix[j], Number) {
            cat := Number;
        } else if Is(suffix[j], Operator) {
            cat := Operator;
        } else {
            return Error, p, "";
        }

        // read token
        var start := j;
        j := j + 1;
        n := n + 1;
        while j != |suffix| && Is(suffix[j], cat)
            decreases if j <= |suffix| then |suffix| - j else j - |suffix|
            invariant suffix == old(suffix)
            invariant m == old(m)
            invariant n == m + j
            invariant p <= j <= |suffix|
            invariant forall i :: p <= i < j ==> Is(suffix[i], cat)
        {
            j := j + 1;
            n := n + 1;
        }
        token := suffix[start..j];
    }

    method Prune() returns ()
        requires Valid()
        modifies this
        ensures Valid()
    {
        suffix := suffix[j..];
        m := m + j;
        j := 0;
    }
}