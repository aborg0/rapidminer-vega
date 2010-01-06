\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{rapidminertutorial}[28/05/2007 RapidMiner Tutorial class]

\LoadClass[a4paper,11pt]{book}

\ProcessOptions*

\RequirePackage{epsfig}
\RequirePackage{fancyhdr}
\RequirePackage{fancyvrb}
\RequirePackage{ifthen}
\RequirePackage{makeidx}
%\RequirePackage{index}
\RequirePackage{tabularx}
\RequirePackage{textcomp}
\RequirePackage{xspace}
\RequirePackage{listings}
% \RequirePackage{ae}
\RequirePackage{longtable}
\RequirePackage[pdftex,bookmarks=true,bookmarksnumbered=true,colorlinks]{hyperref}

%% macro(s) for the name of the machine learning environment:
\newcommand{\rapidminer}{\protect \textsc{RapidMiner}\xspace}
\newcommand{\RapidMiner}{\rapidminer}
\newcommand{\RAPIDMINER}{\rapidminer}
\newcommand{\rapidminerurl}{\url{http://www.rapidminer.com/}}


\newcommand{\SVMlight}{{\em SVM\,}$^{light}$}
\newcommand{\svmlight}{{\em SVM\,}$^{light}$}

\newcommand{\op}[1]{\textsc{#1}}
\newcommand{\refop}[1]{\op{#1} (see section \ref{sec:op:#1})}
\newcommand{\ioobj}[1]{\textsf{#1}}
\newcommand{\tag}[1]{\texttt{#1}}
\newcommand{\xmlcode}[1]{\texttt{#1}}
\newcommand{\filename}[1]{\texttt{#1}}
\newcommand{\commandline}[1]{\texttt{#1}}
\newcommand{\filecont}[1]{\texttt{#1}}
\newcommand{\techterm}[1]{\textit{#1}\index{#1}}
%\newcommand{\url}[1]{\texttt{#1}}
\newcommand{\para}[1]{\textit{#1}}
\newcommand{\parval}[1]{\texttt{#1}}
\newcommand{\java}[1]{\texttt{#1}}
% \newcommand{\fileref}[1]{\textit{#1}}
\newcommand{\http}{http:/\hspace{-0.2em}/}
\newcommand{\useroption}[1]{\textsf{#1}}

\lstset{flexiblecolumns=true}
\lstdefinelanguage{rapidminerxml}{keywords={operator,parameter,list,attribute,label,id,weight,operator,operators,attributeset},ndkeywords={name,class,key,value,sourcecol,valuetype,blocktype,sourcecol_end,sourcefile,default_source,unit,classes,blocknumber}}
\lstdefinestyle{rapidminerxmlstyle}{language=rapidminerxml,ndkeywordstyle=\textit}

\newcommand{\examplefile}[3]{%
\begin{figure}[htbp]%
\lstinputlisting[style=rapidminerxmlstyle,frame=tb,basicstyle=\footnotesize]{#1}%
\caption{#3}%
\label{fig:#2}%
\end{figure}%
}

\newcommand{\examplefileshort}[4]{%
\begin{figure}[htbp]%
\lstinputlisting[style=rapidminerxmlstyle,frame=tb,basicstyle=\footnotesize]{#1}%
\caption[#4]{#3}%
\label{fig:#2}%
\end{figure}%
}

\newcommand{\javafile}[4]{%
\begin{figure}[htbp]%
\lstinputlisting[language=Java,frame=tb,basicstyle=\footnotesize]{#1}%
\caption[#4]{#3}%
\label{fig:#2}%
\end{figure}%
}

\fvset{frame=lines}
\newcommand{\inputfile}[4]{%
\begin{figure}[htbp]%
\VerbatimInput{#1}%
\caption[#4]{#3}%
\label{fig:#2}%
\end{figure}%
}

%% BEGIN Operator Layout 

%% Sectioning commands
\newcommand{\currentoperator}{none}
\newcommand{\currentsuperop}{none}
\newcommand{\superio}

\newcommand{\operator}[2][]{%
  \ifthenelse{\equal{#1}{}}%
  {\subsection{#2}}%
  {\subsection[#2]{#2\\\normalsize{\textit{ extends } #1 (p. \pageref{sec:op:#1})}}}%
  \label{sec:op:#2}%
  \index{#2@{\op{#2\/}}}%
  \renewcommand{\currentoperator}{#2}%
  \renewcommand{\currentsuperop}{#1}%
}

\newcommand{\absoperator}[2][]{%
  \ifthenelse{\equal{#1}{}}%
  {\subsection[\textit{#2}]{\textit{#2} (abstract)}}%
  {\subsection[\textit{#2}]{\textit{#2} (abstract)\\\normalsize{\textit{ extends } #1 (p. \pageref{sec:op:#1})}}}%
  \label{sec:op:#2}%
  \index{#2}%
  \renewcommand{\currentoperator}{#2}%
  \renewcommand{\currentsuperop}{#1}%
}

%% Inner Operators
\newenvironment{innerops}{\noindent\textbf{Inner Operator(s)}\begin{enumerate}}{\end{enumerate}}

%% Input/Output Objects
\newenvironment{inputoutput}[1]
{\begin{minipage}[t]{6cm}\textbf{#1}\list{}{\setlength{\labelwidth}{-1em}\itemindent-\leftmargin
      \let\makelabel\inputoutputlabel\setlength{\itemsep}{0cm}}}
  {\endlist\vspace{1ex}\end{minipage}}
\newcommand*\inputoutputlabel[1]{\hspace\labelsep
  \normalfont\textbullet\ \ioobj{#1}}
\newenvironment{opout}{\begin{inputoutput}{Generated output:}}{\end{inputoutput}}
\newenvironment{opin}{\begin{inputoutput}{Required input:}}{\end{inputoutput}}

%% Parameter und Values
\newenvironment{paramvalues}[1]
               {\noindent\textbf{#1}\list{\textbullet}{\setlength{\labelwidth}{-1em} \itemindent-\leftmargin
                        \let\makelabel\paramvalueslabel\setlength{\itemsep}{0cm}}}
               {\endlist}
\newcommand*\paramvalueslabel[1]{\hspace\labelsep
                                \normalfont #1}
\newenvironment{values}{\begin{paramvalues}{Values:}}{\end{paramvalues}}
\newenvironment{parameters}{\begin{paramvalues}{Parameters:}}{\end{paramvalues}}
\newcommand{\reqpar}[1][]{\item[\textbullet{}\textbf{\ifthenelse{\equal{#1}{}}{}{ #1:}}]}
\newcommand{\optpar}[1][]{\item[\textopenbullet{}\textbf{\ifthenelse{\equal{#1}{}}{}{ #1:}}]}
\newcommand{\val}[1][]{\reqpar[#1]}

%% Group
\newcommand{\group}[1]{\begin{description}\item[Group:] #1 \end{description}}

%% Description
\newcommand{\opdescr}{\paragraph{Description:}}


%% END Operator Layout

%
% Layout
%
\pagestyle{fancy}
\fancyhf{}
\fancyhead[re]{\slshape \leftmark}
\fancyhead[lo]{\slshape \rightmark}
\fancyhead[le,ro]{\thepage}
\fancyfoot[lo]{\footnotesize The \rapidminer 5.0 beta Tutorial}
\fancyfoot[re]{\footnotesize \today}
\renewcommand{\familydefault}{cmss}
\setlength{\parindent}{0cm}
\setlength{\parskip}{6pt}



\newcommand{\address}[1]{\def\theaddress{#1}}
\newcommand{\subtitle}[1]{\def\thesubtitle{#1}}
\subtitle{
User Guide\\[6pt]
Operator Reference\\[6pt]
Developer Tutorial
}
\newcommand{\logo}[1]{\def\printlogo{\includegraphics[width=\linewidth]{#1}}}
\logo{./graphics/rapidminer.pdf}
\newcommand{\nologo}{\def\printlogo{}}

\renewcommand{\maketitle}{
  \thispagestyle{empty}
  {
  \Huge
  \center
  \bfseries \@title\\[7pt]
  }
  {\Large\bfseries
  \begin{center}
    \thesubtitle
  \end{center}
  }

  
  \vfill
  \begin{center}
  \printlogo
  \end{center}
}


\endinput
